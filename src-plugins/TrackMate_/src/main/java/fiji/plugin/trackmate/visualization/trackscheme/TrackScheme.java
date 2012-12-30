package fiji.plugin.trackmate.visualization.trackscheme;

import ij.ImagePlus;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.ImageIcon;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

import org.jgrapht.graph.DefaultWeightedEdge;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.model.mxICell;
import com.mxgraph.model.mxIGraphModel;
import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxEventSource.mxIEventListener;
import com.mxgraph.util.mxStyleUtils;
import com.mxgraph.view.mxGraphSelectionModel;
import com.mxgraph.view.mxPerimeter;
import com.mxgraph.view.mxStylesheet;

import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMateModelChangeEvent;
import fiji.plugin.trackmate.TrackMateModelChangeListener;
import fiji.plugin.trackmate.TrackMateSelectionChangeEvent;
import fiji.plugin.trackmate.TrackMateSelectionChangeListener;
import fiji.plugin.trackmate.visualization.AbstractTrackMateModelView;
import fiji.plugin.trackmate.visualization.TrackMateModelView;

public class TrackScheme implements TrackMateModelChangeListener, TrackMateSelectionChangeListener, TrackMateModelView {

	/*
	 * CONSTANTS
	 */
	private static final boolean DEBUG = false;
	private static final boolean DEBUG_SELECTION = false;
	static final int Y_COLUMN_SIZE = 96;
	static final int X_COLUMN_SIZE = 160;

	static final int DEFAULT_CELL_WIDTH = 128;
	static final int DEFAULT_CELL_HEIGHT = 40;

	public static final ImageIcon 	TRACK_SCHEME_ICON = new ImageIcon(TrackSchemeFrame.class.getResource("resources/track_scheme.png"));
	public static final String 		DEFAULT_COLOR = "#FF00FF";
	private static final Dimension 	DEFAULT_SIZE = new Dimension(800, 600);
	static final String 			DEFAULT_STYLE_NAME = "Full"; 
	static final int 				TABLE_CELL_WIDTH 		= 40;
	static final Color 				GRID_COLOR = Color.GRAY;
	/** Are linking costs displayed by default? Can be changed in the toolbar. */
	static final boolean 			DEFAULT_DO_DISPLAY_COSTS_ON_EDGES = false;
	/** Do we display the background decorations by default? */
	static final boolean 			DEFAULT_DO_PAINT_DECORATIONS = true;
	/** Do we toggle linking mode by default? */
	static final boolean DEFAULT_LINKING_ENABLED = false;

	private static final Map<String, Map<String, Object>> VERTEX_STYLES;
	private static final HashMap<String, Object> BASIC_VERTEX_STYLE = new HashMap<String, Object>();
	private static final HashMap<String, Object> SIMPLE_VERTEX_STYLE = new HashMap<String, Object>();
	private static final HashMap<String, Object> BASIC_EDGE_STYLE = new HashMap<String, Object>();
	static {
		BASIC_VERTEX_STYLE.put(mxConstants.STYLE_FILLCOLOR, "white");
		BASIC_VERTEX_STYLE.put(mxConstants.STYLE_FONTCOLOR, "black");
		BASIC_VERTEX_STYLE.put(mxConstants.STYLE_ALIGN, mxConstants.ALIGN_RIGHT);
		BASIC_VERTEX_STYLE.put(mxConstants.STYLE_SHAPE, mxScaledLabelShape.SHAPE_NAME);
		BASIC_VERTEX_STYLE.put(mxConstants.STYLE_IMAGE_ALIGN, mxConstants.ALIGN_LEFT);
		BASIC_VERTEX_STYLE.put(mxConstants.STYLE_ROUNDED, true);
		BASIC_VERTEX_STYLE.put(mxConstants.STYLE_PERIMETER, mxPerimeter.RectanglePerimeter);
		BASIC_VERTEX_STYLE.put(mxConstants.STYLE_STROKECOLOR, DEFAULT_COLOR);

		SIMPLE_VERTEX_STYLE.put(mxConstants.STYLE_FILLCOLOR, "white");
		SIMPLE_VERTEX_STYLE.put(mxConstants.STYLE_FONTCOLOR, "black");
		SIMPLE_VERTEX_STYLE.put(mxConstants.STYLE_ALIGN, mxConstants.ALIGN_RIGHT);
		SIMPLE_VERTEX_STYLE.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_ELLIPSE);
		SIMPLE_VERTEX_STYLE.put(mxConstants.STYLE_IMAGE_ALIGN, mxConstants.ALIGN_LEFT);
		SIMPLE_VERTEX_STYLE.put(mxConstants.STYLE_ROUNDED, true);
		SIMPLE_VERTEX_STYLE.put(mxConstants.STYLE_PERIMETER, mxPerimeter.EllipsePerimeter);
		SIMPLE_VERTEX_STYLE.put(mxConstants.STYLE_STROKECOLOR, DEFAULT_COLOR);
		SIMPLE_VERTEX_STYLE.put(mxConstants.STYLE_NOLABEL, true);

		BASIC_EDGE_STYLE.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_CONNECTOR);
		BASIC_EDGE_STYLE.put(mxConstants.STYLE_ALIGN, mxConstants.ALIGN_CENTER);
		BASIC_EDGE_STYLE.put(mxConstants.STYLE_VERTICAL_ALIGN, mxConstants.ALIGN_MIDDLE);
		BASIC_EDGE_STYLE.put(mxConstants.STYLE_STARTARROW, mxConstants.NONE);
		BASIC_EDGE_STYLE.put(mxConstants.STYLE_ENDARROW, mxConstants.NONE);
		BASIC_EDGE_STYLE.put(mxConstants.STYLE_STROKEWIDTH, 2.0f);
		BASIC_EDGE_STYLE.put(mxConstants.STYLE_STROKECOLOR, DEFAULT_COLOR);

		VERTEX_STYLES = new HashMap<String, Map<String, Object> >(2);
		VERTEX_STYLES.put(DEFAULT_STYLE_NAME, BASIC_VERTEX_STYLE);
		VERTEX_STYLES.put("Simple", SIMPLE_VERTEX_STYLE);

	}

	/*
	 * FIELDS
	 */

	/** The model this instance is a view of (Yoda I speak like). */
	private TrackMateModel model;
	/** The frame in which we display the TrackScheme GUI. */
	private TrackSchemeFrame gui;
	/** The display settings map. */
	private Map<String, Object> displaySettings = new HashMap<String, Object>();
	/** The JGraphX object that displays the graph. */
	private JGraphXAdapter graph;
	/** The graph layout in charge of re-aligning the cells. */
	private mxTrackGraphLayout graphLayout;
	/** A flag used to prevent double event firing when setting the selection programmatically. */
	private boolean doFireSelectionChangeEvent = true;
	/** A flag used to prevent double event firing when setting the selection programmatically. */
	private boolean doFireModelChangeEvent = true;
	/**  
	 * The current row length for each frame.
	 * That is, for frame <code>i</code>, the number of cells on the row
	 * corresponding to frame <code>i</code> is <code>rowLength.get(i)</code>.
	 * This field is regenerated after each call to {@link #execute(Object)}.
	 */
	private Map<Integer, Integer> rowLengths = new HashMap<Integer, Integer>();
	/** 
	 * Stores the column index that is the first one after all the track columns.
	 * This field is regenerated after each call to {@link #execute(Object)}.
	 */
	private int unlaidSpotColumn = 3;

	/*
	 * CONSTRUCTORS
	 */

	public TrackScheme(final TrackMateModel model)  {
		setModel(model);
		initDisplaySettings();
	}


	/*
	 * METHODS
	 */


	/**
	 * @return the column index that is the first one after all the track columns.
	 */
	public int getUnlaidSpotColumn() {
		return unlaidSpotColumn;
	}

	/**
	 * @return the first free column for the target row.
	 */
	public int getNextFreeColumn(int frame) {
		Integer columnIndex = rowLengths.get(frame);
		if (null == columnIndex) {
			columnIndex = 2;
		}
		return columnIndex+1;
	}

	/**
	 * @return the GUI frame controlled by this class.
	 */
	public TrackSchemeFrame getGUI() {
		return gui;
	}

	/**
	 * @return the {@link JGraphXAdapter} that serves as a model for the graph displayed in this frame.
	 */
	public JGraphXAdapter getGraph() {
		return graph;
	}

	/**
	 * @return the graph layout in charge of arranging the cells on the graph.
	 */
	public mxTrackGraphLayout getGraphLayout() {
		return graphLayout;	
	}

	/**
	 * Used to instantiate and configure the {@link JGraphXAdapter} that will be used for display.
	 * Hook for subclassers.
	 */
	private JGraphXAdapter createGraph() {
		final JGraphXAdapter graph = new JGraphXAdapter(model);
		graph.setAllowLoops(false);
		graph.setAllowDanglingEdges(false);
		graph.setCellsCloneable(false);
		graph.setCellsSelectable(true);
		graph.setCellsDisconnectable(false);
		graph.setCellsMovable(true);
		graph.setGridEnabled(false);
		graph.setLabelsVisible(true);
		graph.setDropEnabled(false);

		mxStylesheet styleSheet = graph.getStylesheet();
		styleSheet.setDefaultEdgeStyle(BASIC_EDGE_STYLE);
		styleSheet.setDefaultVertexStyle(BASIC_VERTEX_STYLE);
		styleSheet.putCellStyle(DEFAULT_STYLE_NAME, BASIC_VERTEX_STYLE);
		styleSheet.putCellStyle("Simple", SIMPLE_VERTEX_STYLE);

		// Set spot image to cell style
		try {
			graph.getModel().beginUpdate();
			for(mxICell cell : graph.getVertexCells()) {
				Spot spot = graph.getSpotFor(cell);
				graph.getModel().setStyle(cell, mxConstants.STYLE_IMAGE+"="+"data:image/base64,"+spot.getImageString());
			}
		} finally {
			graph.getModel().endUpdate();
		}

		// Cells removed from JGraphX
		graph.addListener(mxEvent.CELLS_REMOVED, new CellRemovalListener());

		// Cell selection change
		graph.getSelectionModel().addListener(mxEvent.CHANGE, new SelectionChangeListener());

		// Return graph
		return graph;
	}

	/**
	 * Update or create a cell for the target spot. Is called after the user modified a spot 
	 * (location, radius, ...) somewhere else. 
	 * @param spot  the spot that was modified.
	 */
	private void updateCellOf(final Spot spot) {

		mxICell cell = graph.getCellFor(spot);
		if (DEBUG)
			System.out.println("[TrackSchemeFrame] modelChanged: updating cell for spot "+spot);
		if (null == cell) {
			// mxCell not present in graph. Most likely because the corresponding spot belonged
			// to an invisible track, and a cell was not created for it when TrackScheme was
			// launched. So we create one on the fly now.
			int row = getUnlaidSpotColumn();
			cell = insertSpotInGraph(spot, row);
			int frame = spot.getFeature(Spot.FRAME).intValue();
			rowLengths.put(frame, row+1);
		}

		// Update cell look
		String style = cell.getStyle();
		style = mxStyleUtils.setStyle(style, mxConstants.STYLE_IMAGE, "data:image/base64,"+spot.getImageString());
		graph.getModel().setStyle(cell, style);
		final double dx = model.getSettings().dx;
		long height = Math.min(DEFAULT_CELL_WIDTH, Math.round(2 * spot.getFeature(Spot.RADIUS) / dx ));
		height = Math.max(height, DEFAULT_CELL_HEIGHT/3);
		graph.getModel().getGeometry(cell).setHeight(height);
	}

	/**
	 * Insert a spot in the {@link TrackSchemeFrame}, by creating a {@link mxCell} in the 
	 * graph model of this frame and position it according to its feature.
	 */
	private mxICell insertSpotInGraph(Spot spot, int targetColumn) {
		mxICell cellAdded = graph.getCellFor(spot);
		if (cellAdded != null) {
			// cell for spot already exist, do nothing and return original spot
			return cellAdded;
		}
		// Instantiate JGraphX cell
		cellAdded = graph.addJGraphTVertex(spot);
		// Position it
		int row = spot.getFeature(Spot.FRAME).intValue() + 1;
		double x = (targetColumn-1) * X_COLUMN_SIZE - DEFAULT_CELL_WIDTH/2;
		double y = (0.5 + row) * Y_COLUMN_SIZE - DEFAULT_CELL_HEIGHT/2; 
		double dx = model.getSettings().dx;
		long height = Math.min(DEFAULT_CELL_WIDTH, Math.round(2 * spot.getFeature(Spot.RADIUS) / dx ));
		height = Math.max(height, 12);
		mxGeometry geometry = new mxGeometry(x, y, DEFAULT_CELL_WIDTH, height);
		cellAdded.setGeometry(geometry);
		// Set its style
		graph.getModel().setStyle(cellAdded, mxConstants.STYLE_IMAGE+"="+"data:image/base64,"+spot.getImageString());
		return cellAdded;
	}


	/**
	 * Import a whole track from the {@link TrackMateModel} and make it visible
	 * @param trackIndex  the index of the track to show in TrackScheme 
	 */
	private void importTrack(int trackIndex) {
		model.beginUpdate();
		graph.getModel().beginUpdate();
		try {
			// Flag original track as visible
			model.getTrackModel().setFilteredTrackID(trackIndex, true, false);
			// Find adequate column
			int targetColumn = getUnlaidSpotColumn();
			// Create cells for track
			Set<Spot> trackSpots = model.getTrackModel().getTrackSpots(trackIndex);
			for (Spot trackSpot : trackSpots) {
				int frame = trackSpot.getFeature(Spot.FRAME).intValue();
				int column = Math.max(targetColumn, getNextFreeColumn(frame));
				insertSpotInGraph(trackSpot, column);
				rowLengths.put(frame , column);
			}
			Set<DefaultWeightedEdge> trackEdges = model.getTrackModel().getTrackEdges(trackIndex);
			for (DefaultWeightedEdge trackEdge : trackEdges) {
				graph.addJGraphTEdge(trackEdge);
			}
		} finally {
			model.endUpdate();
			graph.getModel().endUpdate();
		}
	}

	/**
	 * This method is called when the user has created manually an edge in the graph, by dragging
	 * a link between two spot cells. It checks whether the matching edge in the model exists, 
	 * and tune what should be done accordingly.
	 * @param cell  the mxCell of the edge that has been manually created.
	 */
	protected void addEdgeManually(mxCell cell) {
		if (cell.isEdge()) {
			final mxIGraphModel graphModel = graph.getModel();
			cell.setValue("New");
			model.beginUpdate();
			graphModel.beginUpdate();
			try {

				Spot source = graph.getSpotFor(cell.getSource());
				Spot target = graph.getSpotFor(cell.getTarget());

				if (Spot.frameComparator.compare(source, target) == 0) {
					// Prevent adding edges between spots that belong to the same frame

					if (DEBUG) {
						System.out.println("[TrackScheme] addEdgeManually: edge is between 2 spots belonging to the same frame. Removing it.");
					}
					graph.removeCells(new Object[] { cell } );

				} else {
					// We can add it to the model

					// Put them right in order: since we use a oriented graph,
					// we want the source spot to precede in time.
					if (Spot.frameComparator.compare(source, target) > 0) {

						if (DEBUG) {
							System.out.println("[TrackScheme] souce " + source + " succeed target " + target + ". Inverting edge direction.");
						}

						Spot tmp = source;
						source = target;
						target = tmp;
					}
					// We add a new jGraphT edge to the underlying model, if it does not exist yet.
					DefaultWeightedEdge edge = model.getTrackModel().getEdge(source, target); 
					if (null == edge) {
						edge = model.addEdge(source, target, -1);
					} else {
						// Ah. There was an existing edge in the model we were trying to re-add there, from the graph.
						// We remove the graph edge we have added,
						if (DEBUG) {
							System.out.println("[TrackScheme] addEdgeManually: edge pre-existed. Retrieve it.");
						}
						graph.removeCells(new Object[] { cell } );
						// And re-create a graph edge from the model edge.
						cell = graph.addJGraphTEdge(edge);
						cell.setValue(String.format("%.1f", model.getTrackModel().getEdgeWeight(edge)));
						// We also need now to check if the edge belonged to a visible track. If not,
						// we make it visible.
						int index = model.getTrackModel().getTrackIDOf(edge); 
						// This will work, because track indices will be reprocessed only after the graphModel.endUpdate() 
						// reaches 0. So now, it's like we are dealing with the track indices priori to modification.
						if (model.getTrackModel().isTrackFiltered(index)) {
							if (DEBUG) {
								System.out.println("[TrackScheme] addEdgeManually: track was visible. Do nothing.");
							}
						} else {
							if (DEBUG) {
								System.out.println("[TrackScheme] addEdgeManually: track was invisible. Make it visible.");
							}
							importTrack(index);
						}
					}
					graph.mapEdgeToCell(edge, cell);
				}

			} finally {
				model.endUpdate();
				model.getSelectionModel().clearEdgeSelection();
				graphModel.endUpdate();
			}
		}
	}




	/*
	 * OVERRIDEN METHODS
	 */

	@Override
	public void selectionChanged(TrackMateSelectionChangeEvent event) {
		if (DEBUG_SELECTION) 
			System.out.println("[TrackSchemeFrame] selectionChanged: received event "+event.hashCode()+" from "+event.getSource()+". Fire flag is "+doFireSelectionChangeEvent);
		if (!doFireSelectionChangeEvent)
			return;
		doFireSelectionChangeEvent = false;

		/* Performance issue: we do our highlighting here, in batch, bypassing highlight* methods		 */
		{
			SelectionModel selectionModel = model.getSelectionModel();
			ArrayList<Object> newSelection = new ArrayList<Object>(selectionModel.getSpotSelection().size() + selectionModel.getEdgeSelection().size());
			Iterator<DefaultWeightedEdge> edgeIt = selectionModel.getEdgeSelection().iterator();
			while(edgeIt.hasNext()) {
				mxICell cell = graph.getCellFor(edgeIt.next());
				if (null != cell) {
					newSelection.add(cell);
				}
			}
			Iterator<Spot> spotIt = selectionModel.getSpotSelection().iterator();
			while(spotIt.hasNext()) {
				mxICell cell = graph.getCellFor(spotIt.next());
				if (null != cell) {
					newSelection.add(cell);
				}
			}
			mxGraphSelectionModel mGSmodel = graph.getSelectionModel();
			mGSmodel.setCells(newSelection.toArray());
		}

		// Center on selection if we added one spot exactly
		Map<Spot, Boolean> spotsAdded = event.getSpots();
		if (spotsAdded != null && spotsAdded.size() == 1) {
			boolean added = spotsAdded.values().iterator().next();
			if (added) {
				Spot spot = spotsAdded.keySet().iterator().next();
				centerViewOn(spot);
			}
		}
		doFireSelectionChangeEvent = true;
	}

	@Override
	public void centerViewOn(Spot spot) {
		gui.centerViewOn(graph.getCellFor(spot));
	}

	/**
	 * Used to catch spot creation events that occurred elsewhere, for instance by manual editing in 
	 * the {@link AbstractTrackMateModelView}. 
	 * <p>
	 * We have to deal with the graph modification ourselves here, because the {@link TrackMateModel} model
	 * holds a non-listenable JGraphT instance. A modification made to the model would not be reflected
	 * on the graph here.
	 */
	@Override
	public void modelChanged(final TrackMateModelChangeEvent event) {

		// Only catch model changes
		if (event.getEventID() != TrackMateModelChangeEvent.MODEL_MODIFIED)
			return;

		graph.getModel().beginUpdate();
		try {
			ArrayList<mxICell> cellsToRemove = new ArrayList<mxICell>();

			final int targetColumn = getUnlaidSpotColumn();

			// Deal with spots
			if (event.getSpots() != null) {
				for (Spot spot : event.getSpots() ) {

					if (event.getSpotFlag(spot) == TrackMateModelChangeEvent.FLAG_SPOT_ADDED) {

						int frame = spot.getFeature(Spot.FRAME).intValue();
						// Put in the graph
						int column = Math.max(targetColumn, getNextFreeColumn(frame));
						insertSpotInGraph(spot, column); // move in right+1 free column
						rowLengths.put(frame, column);

					} else if (event.getSpotFlag(spot) == TrackMateModelChangeEvent.FLAG_SPOT_MODIFIED) {

						// Change the look of the cell
						updateCellOf(spot);

					}  else if (event.getSpotFlag(spot) == TrackMateModelChangeEvent.FLAG_SPOT_REMOVED) {

						mxICell cell = graph.getCellFor(spot);
						cellsToRemove.add(cell);

					} 
				}
				graph.removeCells(cellsToRemove.toArray(), true);
			}

		} finally {
			graph.getModel().endUpdate();
		}

		// Deal with edges
		if (event.getEdges() != null) {

			graph.getModel().beginUpdate();
			try {
				for (DefaultWeightedEdge edge : event.getEdges()) {
					if (event.getEdgeFlag(edge) == TrackMateModelChangeEvent.FLAG_EDGE_ADDED) {

						mxICell edgeCell = graph.getCellFor(edge);
						if (null == edgeCell) {

							// Make sure target & source cells exist

							Spot source = model.getTrackModel().getEdgeSource(edge);
							mxCell sourceCell = graph.getCellFor(source);
							if (sourceCell == null) {
								int frame = source.getFeature(Spot.FRAME).intValue();
								// Put in the graph
								int targetColumn = getUnlaidSpotColumn();
								int column = Math.max(targetColumn, getNextFreeColumn(frame));
								insertSpotInGraph(source, column); // move in right+1 free column
								rowLengths.put(frame, column);
							}

							Spot target = model.getTrackModel().getEdgeTarget(edge);
							mxCell targetCell = graph.getCellFor(target);
							if (targetCell == null) {
								int frame = target.getFeature(Spot.FRAME).intValue();
								// Put in the graph
								int targetColumn = getUnlaidSpotColumn();
								int column = Math.max(targetColumn, getNextFreeColumn(frame));
								insertSpotInGraph(target, column); // move in right+1 free column
								rowLengths.put(frame, column);
							}


							// And finally create the edge cell
							edgeCell = graph.addJGraphTEdge(edge);


						}

						graph.getModel().add(graph.getDefaultParent(), edgeCell, 0);
						String edgeStyle = edgeCell.getStyle();
						//						edgeStyle = mxStyleUtils.setStyle(edgeStyle, mxSideTextShape.STYLE_DISPLAY_COST, ""+graphLayout.isDoDisplayCosts());
						graph.getModel().setStyle(edgeCell, edgeStyle);

					}
				}
			} finally {
				graph.getModel().endUpdate();
			}
		}
	}

	@Override
	public Map<String, Object> getDisplaySettings() {
		return displaySettings;
	}

	@Override
	public void setDisplaySettings(String key, Object value) {
		displaySettings.put(key, value);
	}

	@Override
	public Object getDisplaySettings(String key) {
		return displaySettings.get(key);
	}

	@Override
	public void render() {
		SwingUtilities.invokeLater(new Runnable(){
			public void run()	{
				initGUI();
				doTrackLayout();
			}
		});
	}

	@Override
	public void refresh() {
		System.out.println("[TrackScheme] refresh() called");
	}

	@Override
	public void clear() {
		System.out.println("[TrackScheme] clear() called");
	}

	@Override
	public String getInfoText() {
		return "<html>InfoText for TrachScheme is not redacted.</html>";
	}

	@Override
	public TrackMateModel getModel() {
		return model;
	}

	/*
	 * PROTECTED METHODS
	 */

	protected void initDisplaySettings() {
		displaySettings.put(KEY_SPOTS_VISIBLE, true);
		displaySettings.put(KEY_DISPLAY_SPOT_NAMES, false);
		displaySettings.put(KEY_SPOT_COLOR_FEATURE, null);
		displaySettings.put(KEY_SPOT_RADIUS_RATIO, 1.0f);
		displaySettings.put(KEY_TRACKS_VISIBLE, true);
		displaySettings.put(KEY_TRACK_DISPLAY_MODE, DEFAULT_TRACK_DISPLAY_MODE);
		displaySettings.put(KEY_TRACK_DISPLAY_DEPTH, DEFAULT_TRACK_DISPLAY_DEPTH);
		displaySettings.put(KEY_COLORMAP, DEFAULT_COLOR_MAP);
	}


	/*
	 * PRIVATE METHODS
	 */

	/**
	 * Called when the user makes a selection change in the graph. Used to forward this event 
	 * to the {@link InfoPane} and to other {@link TrackMateSelectionChangeListener}s.
	 * @param model the selection model 
	 * @param added  the cells  <b>removed</b> from selection (careful, inverted)
	 * @param removed  the cells <b>added</b> to selection (careful, inverted)
	 */
	private void userChangedSelection(mxGraphSelectionModel mxGSmodel, Collection<Object> added, Collection<Object> removed) { // Seems to be inverted
		if (!doFireSelectionChangeEvent)
			return;
		Collection<Spot> spotsToAdd = new ArrayList<Spot>();
		Collection<Spot> spotsToRemove = new ArrayList<Spot>();
		Collection<DefaultWeightedEdge> edgesToAdd = new ArrayList<DefaultWeightedEdge>();
		Collection<DefaultWeightedEdge> edgesToRemove = new ArrayList<DefaultWeightedEdge>();

		if (null != added) {
			for(Object obj : added) {
				mxCell cell = (mxCell) obj;

				if (cell.getChildCount() > 0) {

					for (int i = 0; i < cell.getChildCount(); i++) {
						mxICell child = cell.getChildAt(i);
						if (child.isVertex()) {
							Spot spot = graph.getSpotFor(child);
							spotsToRemove.add(spot);
						} else {
							DefaultWeightedEdge edge = graph.getEdgeFor(child);
							edgesToRemove.add(edge);
						}
					}

				} else {

					if (cell.isVertex()) {
						Spot spot = graph.getSpotFor(cell);
						spotsToRemove.add(spot);
					} else {
						DefaultWeightedEdge edge = graph.getEdgeFor(cell);
						edgesToRemove.add(edge);
					}
				}
			}
		}

		if (null != removed) {
			for(Object obj : removed) {
				mxCell cell = (mxCell) obj;

				if (cell.getChildCount() > 0) {

					for (int i = 0; i < cell.getChildCount(); i++) {
						mxICell child = cell.getChildAt(i);
						if (child.isVertex()) {
							Spot spot = graph.getSpotFor(child);
							spotsToAdd.add(spot);
						} else {
							DefaultWeightedEdge edge = graph.getEdgeFor(child);
							edgesToAdd.add(edge);
						}
					}

				} else {

					if (cell.isVertex()) {
						Spot spot = graph.getSpotFor(cell);
						spotsToAdd.add(spot);
					} else {
						DefaultWeightedEdge edge = graph.getEdgeFor(cell);
						edgesToAdd.add(edge);
					}
				}
			}
		}
		if (DEBUG_SELECTION)
			System.out.println("[TrackScheme] userChangeSelection: sending selection change to model.");
		doFireSelectionChangeEvent = false;
		SelectionModel selectionModel = model.getSelectionModel();
		if (!edgesToAdd.isEmpty())
			selectionModel.addEdgeToSelection(edgesToAdd);
		if (!spotsToAdd.isEmpty())
			selectionModel.addSpotToSelection(spotsToAdd);
		if (!edgesToRemove.isEmpty())
			selectionModel.removeEdgeFromSelection(edgesToRemove);
		if (!spotsToRemove.isEmpty())
			selectionModel.removeSpotFromSelection(spotsToRemove);
		doFireSelectionChangeEvent = true;
	}

	private void initGUI() {
		// Graph to mirror model
		this.graph = createGraph();
		this.gui = new TrackSchemeFrame(graph, model, this);
		this.graphLayout = new mxTrackGraphLayout(graph, model, gui.graphComponent);
		String title = "TrackScheme";
		if (null != model.getSettings().imp)
			title += ": "+model.getSettings().imp.getTitle();
		gui.setTitle(title);
		gui.setSize(DEFAULT_SIZE);
		gui.setVisible(true);

	}



	/*
	 *  INNER CLASSES
	 */

	private class CellRemovalListener implements mxIEventListener {

		public void invoke(Object sender, mxEventObject evt) {

			if (DEBUG)
				System.out.println("[TrackScheme] CellRemovalListener: cells removed - Source of event is "+sender.getClass()+". Fire flag is "+doFireModelChangeEvent);

			if (!doFireModelChangeEvent)
				return;

			// Separate spots from edges
			Object[] objects = (Object[]) evt.getProperty("cells");
			ArrayList<Spot> spotsToRemove = new ArrayList<Spot>();
			ArrayList<Integer> fromFrames = new ArrayList<Integer>();
			ArrayList<DefaultWeightedEdge> edgesToRemove = new ArrayList<DefaultWeightedEdge>();
			for(Object obj : objects) {
				mxCell cell = (mxCell) obj;
				if (null != cell) {
					if (cell.isVertex()) {
						// Build list of removed spots 
						Spot spot = graph.getSpotFor(cell);
						Integer frame = model.getSpots().getFrame(spot);
						if (frame == null) {
							// Already removed; second call to event, have to skip it
							continue;
						}
						spotsToRemove.add(spot);
						fromFrames.add(frame);
						// Clean maps 
						graph.removeMapping(spot);
					} else if (cell.isEdge()) {
						// Build list of removed edges 
						DefaultWeightedEdge edge = graph.getEdgeFor(cell);
						if (null ==edge)
							continue;
						edgesToRemove.add(edge);
						// Clean maps
						graph.removeMapping(edge);
					}
				}
			}

			evt.consume();

			// Clean model
			doFireModelChangeEvent = false;
			model.beginUpdate();
			try {
				model.getSelectionModel().clearSelection();
				// We remove edges first so that we ensure we do not end having orphan edges.
				// Normally JGraphT handles that well, but we enforce things here. To be sure.
				for (DefaultWeightedEdge edge : edgesToRemove) {
					model.removeEdge(edge);
				}
				for (Spot spot : spotsToRemove)  {
					model.removeSpotFrom(spot, null); 
				}

			} finally {
				model.endUpdate();
			}
			doFireModelChangeEvent = true;
		}


	}

	private class SelectionChangeListener implements mxIEventListener {

		@SuppressWarnings("unchecked")
		public void invoke(Object sender, mxEventObject evt) {
			if (DEBUG_SELECTION)
				System.out.println("[TrackSchemeFrame] SelectionChangeListener: selection changed by "+sender+". Fire event flag is "+doFireSelectionChangeEvent);
			if (!doFireSelectionChangeEvent || sender != graph.getSelectionModel())
				return;
			mxGraphSelectionModel model = (mxGraphSelectionModel) sender;
			Collection<Object> added = (Collection<Object>) evt.getProperty("added");
			Collection<Object> removed = (Collection<Object>) evt.getProperty("removed");
			userChangedSelection(model, added, removed);
		}
	}

	/*
	 * ACTIONS
	 * called from gui parts
	 */

	/**
	 * Toggle whether drag-&-drop linking is allowed.
	 * @return  the current settings value.
	 */
	public boolean toggleLinking() {
		boolean enabled = gui.graphComponent.getConnectionHandler().isEnabled();
		gui.graphComponent.getConnectionHandler().setEnabled(!enabled);
		return !enabled;
	}

	public void zoomIn() {
		gui.graphComponent.zoomIn();
	}

	public void zoomOut() {
		gui.graphComponent.zoomOut();
	}

	public void resetZoom() {
		gui.graphComponent.zoomTo(1.0, false);
	}

	public void doTrackLayout() {
		// Position cells
		graphLayout.execute(null);

		SwingUtilities.invokeLater(new Runnable(){
			public void run() {
				gui.graphComponent.refresh();
				gui.graphComponent.repaint();
			}
		});
		// 
		rowLengths = graphLayout.getRowLengths();
		int maxLength = 2;
		for (int rowLength : rowLengths.values()) {
			if (maxLength < rowLength) {
				maxLength = rowLength;
			}
		}
		unlaidSpotColumn = maxLength;
	}

	public void captureUndecorated() {
		BufferedImage image = mxCellRenderer.createBufferedImage(graph, null, 1, Color.WHITE, true, null, 
				gui.graphComponent.getCanvas());
		ImagePlus imp = new ImagePlus("TrackScheme capture", image);
		imp.show();
	}

	public void captureDecorated() {
		JViewport view = gui.graphComponent.getViewport();
		Point currentPos = view.getViewPosition();
		view.setViewPosition(new Point(0, 0)); // We have to do that otherwise, top left is not painted
		Dimension size = view.getViewSize();
		BufferedImage image =  (BufferedImage) view.createImage(size.width, size.height);
		Graphics2D captureG = image.createGraphics();
		view.paintComponents(captureG);
		view.setViewPosition(currentPos);
		ImagePlus imp = new ImagePlus("TrackScheme capture", image);
		imp.show();	
	}

	public boolean toggleDisplayDecoration() {
		boolean enabled = gui.graphComponent.isDoPaintDecorations();
		gui.graphComponent.setDoPaintDecorations(!enabled);
		gui.graphComponent.repaint();
		return !enabled;
	}



	/**
	 * Create links between all the spots currently in the {@link TrackMateModel} selection.
	 * We update simultaneously the {@link TrackMateModel} and the 
	 * {@link JGraphXAdapter}.
	 */
	public void linkSpots() {

		// Sort spots by time
		TreeMap<Integer, Spot> spotsInTime = new TreeMap<Integer, Spot>();
		for (Spot spot : model.getSelectionModel().getSpotSelection()) {
			spotsInTime.put(spot.getFeature(Spot.FRAME).intValue(), spot);
		}

		// Find adequate column
		int targetColumn = getUnlaidSpotColumn();

		// Then link them in this order
		model.beginUpdate();
		graph.getModel().beginUpdate();
		try {
			Iterator<Integer> it = spotsInTime.keySet().iterator();
			Integer previousTime = it.next();
			Spot previousSpot = spotsInTime.get(previousTime);
			// If this spot belong to an invisible track, we make it visible
			Integer index = model.getTrackModel().getTrackIDOf(previousSpot);
			if (index != null && !model.getTrackModel().isTrackFiltered(index)) {
				importTrack(index);
			}

			while(it.hasNext()) {
				Integer currentTime = it.next();
				Spot currentSpot = spotsInTime.get(currentTime);
				// If this spot belong to an invisible track, we make it visible
				index = model.getTrackModel().getTrackIDOf(currentSpot);
				if (index != null && !model.getTrackModel().isTrackFiltered(index)) {
					importTrack(index);
				}
				// Check that the cells matching the 2 spots exist in the graph
				mxICell currentCell = graph.getCellFor(currentSpot);
				if (null == currentCell) {
					currentCell = insertSpotInGraph(currentSpot, targetColumn);
					if (DEBUG) {
						System.out.println("[TrackScheme] linkSpots: creating cell "+currentCell+" for spot "+currentSpot);
					}
				}
				mxICell previousCell = graph.getCellFor(previousSpot);
				if (null == previousCell) {
					int frame = previousSpot.getFeature(Spot.FRAME).intValue();
					int column = Math.max(targetColumn, getNextFreeColumn(frame));
					rowLengths.put(frame , column);
					previousCell = insertSpotInGraph(previousSpot, column);
					if (DEBUG) {
						System.out.println("[TrackScheme] linkSpots: creating cell "+previousCell+" for spot "+previousSpot);
					}
				}
				// Check if the model does not have already a edge for these 2 spots (that is 
				// the case if the 2 spot are in an invisible track, which track scheme does not
				// know of).
				DefaultWeightedEdge edge = model.getTrackModel().getEdge(previousSpot, currentSpot); 
				if (null == edge) {
					// We create a new edge between 2 spots, and pair it with a new cell edge.
					edge = model.addEdge(previousSpot, currentSpot, -1);
					mxCell cell = (mxCell) graph.addJGraphTEdge(edge);
					cell.setValue("New");
				} else {
					// We retrieve the edge, and pair it with a new cell edge.
					mxCell cell = (mxCell) graph.addJGraphTEdge(edge);
					cell.setValue(String.format("%.1f", model.getTrackModel().getEdgeWeight(edge)));
					// Also, if the existing edge belonged to an existing invisible track, we make it visible.
					index = model.getTrackModel().getTrackIDOf(edge);
					if (index != null && !model.getTrackModel().isTrackFiltered(index)) {
						importTrack(index);
					}
				}
				previousSpot = currentSpot;
			}
		} finally {
			graph.getModel().endUpdate();
			model.endUpdate();
		}	
	}

	/**
	 * Remove the cell selected by the user in the GUI.
	 */
	public void removeSelectedCells() {
		graph.getModel().beginUpdate();
		try {
			graph.removeCells(graph.getSelectionCells());
			// Will be caught by the graph listeners
		} finally {
			graph.getModel().endUpdate();
		}
	}


	private void setModel(TrackMateModel model) {
		// Model listeners
		if (null != this.model) {
			this.model.removeTrackMateModelChangeListener(this);
			this.model.removeTrackMateSelectionChangeListener(this);
		}
		this.model = model;
		this.model.addTrackMateModelChangeListener(this);
		this.model.addTrackMateSelectionChangeListener(this);
	}

	public void selectTrack(final Collection<mxCell> vertices, final Collection<mxCell> edges, final int direction) {

		// Look for spot and edges matching given mxCells
		HashSet<Spot> inspectionSpots = new HashSet<Spot>(vertices.size());
		for(mxCell cell : vertices) {
			Spot spot = graph.getSpotFor(cell);
			if (null == spot) {
				if (DEBUG) {
					System.out.println("[TrackScheme] selectWholeTrack: tried to retrieve cell "+cell+", unknown to spot map.");
				}
				continue;
			}
			inspectionSpots.add(spot);
		}
		HashSet<DefaultWeightedEdge> inspectionEdges = new HashSet<DefaultWeightedEdge>(edges.size());
		for(mxCell cell : edges) {
			DefaultWeightedEdge dwe = graph.getEdgeFor(cell);
			if (null == dwe) {
				if (DEBUG) {
					System.out.println("[TrackScheme] select whole track: tried to retrieve cell "+cell+", unknown to edge map.");
				}
				continue;
			}
			inspectionEdges.add(dwe);
		}
		// Forward to selection model
		model.getSelectionModel().selectTrack(inspectionSpots, inspectionEdges, direction);
	}

}
