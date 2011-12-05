package synopticgwt.client;

import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.BeforeSelectionEvent;
import com.google.gwt.event.logical.shared.BeforeSelectionHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TabPanel;

import synopticgwt.client.input.InputTab;
import synopticgwt.client.invariants.InvariantsTab;
import synopticgwt.client.model.ModelTab;
import synopticgwt.client.util.AnalyticsTracker;
import synopticgwt.client.util.ModelResizeHandler;
import synopticgwt.client.util.ProgressWheel;
import synopticgwt.shared.GWTGraph;
import synopticgwt.shared.GWTInvariantSet;

/**
 * This class implements the entry point to the application -- it contains the
 * first method that will be run when the application is loaded by the browser.
 * This class ties together the various application tabs, and is the central
 * component through which functionality in different tabs communicate with each
 * other.
 */
public class SynopticGWT implements EntryPoint {
    /** Default global logger to use for logging all messages to the console. */
    // public static Logger logger = Logger.getLogger("SynopticGWT");

    /** Create an RPC proxy to talk to the Synoptic service */
    private final ISynopticServiceAsync synopticService = GWT
            .create(ISynopticService.class);

    /** This contains the three main application tabs. */
    TabPanel tabPanel = new TabPanel();

    // TODO: there should be a pWheel for every tab.
    /** The progress wheel is an visual indicator of progress for the user. */
    private ProgressWheel pWheel = null;

    /**
     * This static variable allows other class instances to find/use the
     * singleton SynopticGWT instance.
     */
    public static SynopticGWT entryPoint = null;

    /** Encapsulates logic associated with the input tab (e.g., log, reg exps). */
    private InputTab inputTab = null;

    /** Encapsulates logic having to do with the invariants tab */
    private InvariantsTab invTab = null;

    /** Encapsulates logic having to do with the model (e.g., vis, refinement..) */
    private ModelTab modelTab = null;

    private Map<Integer, Tab<?>> tabIndexToTab = new LinkedHashMap<Integer, Tab<?>>();

    boolean invSetChanged = false;

    /**
     * Entry point to the entire application.
     */
    @Override
    public void onModuleLoad() {
        // NOTE: An entry point is instantiated only once and onModuleLoad() is
        // also called once, so there is no issue in storing the entry point
        // reference in a static field. In addition, there is no multi-threading
        // in GWT, via:
        // http://groups.google.com/group/google-web-toolkit/browse_thread/thread/5a2335ffb117bd08
        SynopticGWT.entryPoint = this;

        // logger.setLevel(Level.FINEST);

        // Add the panel of tabs to the page.
        RootPanel.get("mainDiv").add(tabPanel);

        // Create a new progress wheel object, and associate it with the
        // progressWheelDiv container.
        pWheel = new ProgressWheel("progressWheelDiv");

        // Tab on which the user will submit inputs.
        inputTab = new InputTab(synopticService, pWheel);
        // Tab that will display miner invariants.
        invTab = new InvariantsTab(synopticService, pWheel);
        // Tab that will display the model.
        modelTab = new ModelTab(synopticService, pWheel);

        // Associate the tabs with the tab panel.
        tabPanel.add(inputTab.getPanel(), "Inputs");
        tabPanel.add(invTab.getPanel(), "Invariants");
        tabPanel.add(modelTab.getPanel(), "Model");

        // Build up a map between the tab index in the tab panel and the tab --
        // this map is useful when processing events that change the selected
        // tab.
        tabIndexToTab.put(tabPanel.getWidgetIndex(inputTab.getPanel()),
                inputTab);
        tabIndexToTab.put(tabPanel.getWidgetIndex(invTab.getPanel()), invTab);
        tabIndexToTab.put(tabPanel.getWidgetIndex(modelTab.getPanel()),
                modelTab);

        // logger.info(tabIndexToTab.toString());
        tabPanel.setWidth("100%");
        tabPanel.selectTab(0);
        // Disable the invariants/model tabs (until the user parses a log).
        tabPanel.getTabBar().setTabEnabled(1, false);
        tabPanel.getTabBar().setTabEnabled(2, false);

        tabPanel.addSelectionHandler(new SelectionHandler<Integer>() {
            @Override
            public void onSelection(SelectionEvent<Integer> event) {
                tabSelected(event);
            }
        });

        tabPanel.addBeforeSelectionHandler(new BeforeSelectionHandler<Integer>() {
            @Override
            public void onBeforeSelection(BeforeSelectionEvent<Integer> event) {
                tabBeforeSelected(event);
            }
        });

        // Add handler for when the window is resized while viewing the model.
        // wait until 200 milliseconds after the last window update event
        // to redraw the model.
        // TODO: Have the handler enabled only when the model tab is selected.
        // That is, register the handler when the model tab is clicked, and
        // remove it when any one of the other tabs is clicked.
        Window.addResizeHandler(new ModelResizeHandler(tabPanel.getTabBar(),
                modelTab, 200));
    }

    /**
     * Used by the invariants tab to signal that the user has modified the
     * invariant set, by e.g., activating/deactivating some of the invariants.
     */
    public void invSetChanged() {
        invSetChanged = true;
    }

    /** Called when commit invariants call to the Synoptic service fails. */
    public void commitInvsFailure(Throwable caught) {
        Label error = new Label(
                "Remote Procedure Call Failure while updating invariants: "
                        + caught.toString());
        error.setStyleName("ErrorMessage");
        RootPanel rpcErrorDiv = RootPanel.get("rpcErrorDiv");
        rpcErrorDiv.clear();
        rpcErrorDiv.add(error);
    }

    /** Called when commit invariants call to the Synoptic service succeeds. */
    public void commitInvsSuccess(GWTGraph gwtGraph) {
        invSetChanged = false;
        tabPanel.selectTab(2);
        modelTab.showGraph(gwtGraph);
    }

    /**
     * Fired by SynopticTabPanel _before_ the tab is selected. We capture this
     * event for two reasons: (1) cancel the event in case the tab is disabled,
     * and (2) to track the event for analytics.
     */
    public void tabBeforeSelected(BeforeSelectionEvent<Integer> event) {
        if (!tabIndexToTab.containsKey(event.getItem())) {
            return;
        }
        // 1. Check if the tab is enabled. If not, cancel the event.
        if (!tabPanel.getTabBar().isTabEnabled(event.getItem())) {
            event.cancel();
            return;
        }
        // 2. Only track the event if it has not been canceled.
        Tab<?> t = tabIndexToTab.get(event.getItem());
        AnalyticsTracker.trackEvent(t.trackerCategoryName, "selected",
                "navigation");
    }

    /**
     * Fired by SynopticTabPanel whenever a tab is selected.
     */
    public void tabSelected(SelectionEvent<Integer> event) {
        int tabId = event.getSelectedItem();
        if (tabIndexToTab.get(tabId) != modelTab) {
            return;
        }

        if (!invSetChanged) {
            return;
        }

        // If we are clicking on the model tab, and the invariant set has
        // changed, then we (1) ask the server to re-do refinement/coarsening
        // with the new set of invariants, and (2) re-draw everything in the
        // model tab.

        // ////////////////////// Call to remote service.
        try {
            synopticService.commitInvariants(invTab.activeInvsHashes,
                    new AsyncCallback<GWTGraph>() {
                        @Override
                        public void onFailure(Throwable caught) {
                            commitInvsFailure(caught);
                        }

                        @Override
                        public void onSuccess(GWTGraph gwtGraph) {
                            commitInvsSuccess(gwtGraph);
                        }
                    });
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // //////////////////////
    }

    /**
     * Called by the InputTab whenever the user clicks a button to parse the
     * log. This method creates the invariant/model tabs and initializes them
     * with the result of the parseLog() call to the Synoptic service -- namely,
     * the invariants and the initial model.
     * 
     * @param logInvs
     *            The invariants returned by the service corresponding to the
     *            input log.
     * @param initialModel
     *            The initial model for the log.
     */
    public void logParsed(GWTInvariantSet logInvs, GWTGraph initialModel) {
        // Enable the invariants tab, and show the invariants.
        tabPanel.getTabBar().setTabEnabled(1, true);
        invTab.showInvariants(logInvs);

        // TODO: Communicate whether we are processing a TO or a PO log
        // explicitly, instead of through (initialModel =?= null).
        if (initialModel != null) {
            // TO log.
            // Enable the model tab.
            tabPanel.getTabBar().setTabEnabled(2, true);

            // The modelTab MUST be selected before calling showGraph().
            tabPanel.selectTab(2);
            modelTab.showGraph(initialModel);
            // Retrieve and show the final model.
            modelTab.getFinalModelButtonClick(null);
        } else {
            // PO log.
            // Switch to the invariant tab, and disable the model tab.
            tabPanel.selectTab(1);
            tabPanel.getTabBar().setTabEnabled(2, false);
            // TODO: we also want to clear model state here, in the case
            // that the prior generated model is large.
        }
    }
}
