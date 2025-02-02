/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract DG133W-05-CQ-1067 with the US Government.
 * 
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 * 
 * Contractor Name:        Raytheon Company
 * Contractor Address:     6825 Pine Street, Suite 340
 *                         Mail Stop B8
 *                         Omaha, NE 68106
 *                         402.291.0100
 * 
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package com.raytheon.viz.ui.perspectives;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.advanced.MArea;
import org.eclipse.e4.ui.model.application.ui.advanced.MPerspective;
import org.eclipse.e4.ui.model.application.ui.advanced.MPlaceholder;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartSashContainerElement;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.IPresentationEngine;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.StatusLineManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPageListener;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.localization.exception.LocalizationException;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.util.SizeUtil;
import com.raytheon.uf.viz.core.ContextManager;
import com.raytheon.uf.viz.core.IDisplayPane;
import com.raytheon.uf.viz.core.IDisplayPaneContainer;
import com.raytheon.uf.viz.core.VizApp;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.procedures.Procedure;
import com.raytheon.uf.viz.core.procedures.ProcedureXmlManager;
import com.raytheon.viz.ui.actions.LoadPerspectiveHandler;
import com.raytheon.viz.ui.color.BackgroundColor;
import com.raytheon.viz.ui.color.IBackgroundColorChangedListener;
import com.raytheon.viz.ui.editor.AbstractEditor;
import com.raytheon.viz.ui.tools.AbstractModalTool;
import com.raytheon.viz.ui.tools.ModalToolManager;

/**
 * Manager for generic perspectives. Default implementation for general GUI
 * interface management.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jul 22, 2008  1223     randerso  Initial creation
 * Mar 26, 2013  1799     bsteffen  Fix pan/zoom when in views.
 * Jun 19, 2013  2116     bsteffen  Do not deactivate contexts for parts when
 *                                  closing an inactive perspective.
 * Jan 14, 2014  2594     bclement  added low memory notification
 * Jun 05, 2015  4401     bkowal    Renamed LoadSerializedXml to
 *                                  LoadPerspectiveHandler.
 * Dec 14, 2015  5193     bsteffen  Updates to handle changed eclipse 4 listener
 *                                  calls.
 * Feb 09, 2016  5267     bsteffen  Workaround eclipse 4 poor support for non
 *                                  restorable views.
 * Feb 10, 2016  5329     bsteffen  Close saved editors when deactivating while
 *                                  closing.
 * Jul 11, 2016  5751     bsteffen  Fix timing of tool activation when a part is
 *                                  opened.
 * Sep 01, 2016  5854     bsteffen  Fix closing saved editor in hidden
 *                                  perspective when workbench is closing.
 * Oct 25, 2016  5929     bsteffen  Ensure compatibility layer listeners fire
 *                                  when editors swap
 * Nov 23, 2016  6004     bsteffen  Move handling of nonrestorable views out of this class.
 * Mar 02, 2017 6162        bsteffen    activate/deactivate tools when changing perspectives.
 * 
 * </pre>
 * 
 * @author randerso
 */
public abstract class AbstractVizPerspectiveManager
        implements IBackgroundColorChangedListener {
    protected static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(AbstractVizPerspectiveManager.class);

    private static class PerspectivePageListener implements IPageListener {

        @Override
        public void pageActivated(IWorkbenchPage page) {
        }

        @Override
        public void pageClosed(IWorkbenchPage page) {
            page.removePartListener(partListener);
        }

        @Override
        public void pageOpened(IWorkbenchPage page) {
            page.addPartListener(partListener);
        }

    }

    private static class PerspectivePartListener implements IPartListener {

        @Override
        public void partActivated(IWorkbenchPart part) {
            // update editor on last selected modal tool
            if (part instanceof IEditorPart
                    && part instanceof IDisplayPaneContainer) {
                AbstractVizPerspectiveManager mgr = VizPerspectiveListener
                        .getCurrentPerspectiveManager();
                if (mgr != null) {
                    for (AbstractModalTool tool : mgr.getToolManager()
                            .getSelectedModalTools()) {
                        if (tool != null && tool.getCurrentEditor() != part) {
                            tool.deactivate();
                            tool.setEditor((IDisplayPaneContainer) part);
                            tool.activate();
                        }
                    }
                }

            }
        }

        @Override
        public void partBroughtToTop(IWorkbenchPart part) {
            partActivated(part);
        }

        @Override
        public void partClosed(IWorkbenchPart part) {
        }

        @Override
        public void partDeactivated(IWorkbenchPart part) {
            // update editor on last selected modal tool
            if (part instanceof IEditorPart
                    && part instanceof IDisplayPaneContainer) {
                AbstractVizPerspectiveManager mgr = VizPerspectiveListener
                        .getCurrentPerspectiveManager();
                IWorkbenchPart newPart = part.getSite().getPage()
                        .getActivePart();
                if (newPart instanceof IEditorPart) {
                    if (mgr != null) {
                        for (AbstractModalTool tool : mgr.getToolManager()
                                .getSelectedModalTools()) {
                            if (tool.getCurrentEditor() == part) {
                                tool.deactivate();
                            }
                        }
                    }
                }
            }
        }

        @Override
        public void partOpened(IWorkbenchPart part) {
            // update editor on last selected modal tool
            if (part instanceof IEditorPart
                    && part instanceof IDisplayPaneContainer) {
                final AbstractVizPerspectiveManager mgr = VizPerspectiveListener
                        .getCurrentPerspectiveManager();
                if (mgr != null && !mgr.opened && mgr.getToolManager()
                        .getSelectedModalTools().isEmpty()) {
                    final AbstractEditor editor = (AbstractEditor) part;
                    /*
                     * Need to delay activation so that other part listeners
                     * have time to run and activate part specific contexts
                     * before activating tools. Otherwise tools may not be valid
                     * because the context is not active.
                     */
                    VizApp.runAsync(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                mgr.activateDefaultTool(
                                        editor.getDefaultTool());
                                if (mgr.getToolManager().getSelectedModalTools()
                                        .isEmpty()) {
                                    /*
                                     * Hack due to tool activation not sending
                                     * whether it should be activated or
                                     * deactivated and is just toggling instead.
                                     * TODO: Make AbstractModalTool required
                                     * command parameter for activate or
                                     * deactivate
                                     */
                                    mgr.activateDefaultTool(
                                            editor.getDefaultTool());
                                }
                            } catch (VizException e) {
                                statusHandler.handle(Priority.SIGNIFICANT,
                                        "Error activating tool set", e);
                            }

                        }
                    });
                }
            }
        }
    }

    public static IPartListener partListener = new PerspectivePartListener();

    public static IPageListener pageListener = new PerspectivePageListener();

    /** The window the perspective is loaded to */
    protected IWorkbenchWindow perspectiveWindow;

    protected static IWorkbenchPage page;

    /** Saved editors for the perspective */
    protected List<MPartSashContainerElement> savedEditorAreaUI = new ArrayList<>();

    private IEditorPart activeEditor;

    /** Has the perspective been opened */
    protected boolean opened = false;

    /** StatusLineManager for the window */
    protected IStatusLineManager statusLine;

    /** Perspective id the manager is managing */
    protected String perspectiveId;

    /** The tool manager for the perspective */
    protected ModalToolManager toolManager;

    /** True if the editors should be saved when switching perspectives */
    protected boolean saveEditors = false;

    /** List of perspective dialogs */
    protected List<IPerspectiveSpecificDialog> perspectiveDialogs;

    private List<IContributionItem> items = new ArrayList<IContributionItem>();

    private BackgroundColor backgroundColor;

    private String title;

    /**
     * Clean up savedEditorAreaUI when a perspective is closing. The saved
     * editor area is not disposed correctly by E4 because the saved elements
     * have been removed from the model. The saved area must be disposed before
     * the perspective context is disposed because the perspective context will
     * dispose of the editor context without disposing of the model elements.
     * The {@link #close()} method is called after the perspective context is
     * disposed so it is too late. This handler triggers when the widget is
     * removed from the perspective which happens before the context is
     * disposed.
     */
    private final EventHandler closeHandler = new EventHandler() {

        @Override
        public void handleEvent(Event event) {
            if (savedEditorAreaUI.isEmpty()) {
                return;
            }
            if (event.getProperty(UIEvents.EventTags.NEW_VALUE) != null) {
                return;
            }
            Object element = event.getProperty(UIEvents.EventTags.ELEMENT);
            if (!(element instanceof MPerspective)) {
                return;
            }
            MPerspective perspective = (MPerspective) element;
            if (!perspective.getElementId().equals(perspectiveId)) {
                return;
            }
            IPresentationEngine presentation = perspectiveWindow
                    .getService(IPresentationEngine.class);
            EModelService modelService = perspectiveWindow
                    .getService(EModelService.class);
            for (MUIElement saved : savedEditorAreaUI) {
                List<MPart> covers = modelService.findElements(saved, "cover",
                        MPart.class, null);
                for (MPart cover : covers) {
                    cover.getParent().getChildren().remove(cover);
                }
                presentation.removeGui(saved);
            }
            savedEditorAreaUI.clear();
        }
    };

    public AbstractVizPerspectiveManager() {
        // new up a tool manager for the perspective
        toolManager = new ModalToolManager();
        perspectiveDialogs = new CopyOnWriteArrayList<IPerspectiveSpecificDialog>();
    }

    /**
     * Override this method to have items automatically added/removed from the
     * status line on activation/deactivation. This method will be called when
     * the perspective is opened. Items will be disposed and removed when closed
     * and just removed when deactivated. Always return new items from this
     * method as they will be automatically disposed
     * 
     * @return
     */
    protected List<ContributionItem> getStatusLineItems() {
        return new ArrayList<ContributionItem>();
    }

    /**
     * This is called when the perspective is opened from scratch
     */
    protected abstract void open();

    public void close() {
        if (opened) {
            opened = false;

            closeDialogs();
            deactivateContexts();
            removeFromStatusLine();

            if (backgroundColor != null) {
                backgroundColor.removeListener(BGColorMode.GLOBAL, this);
            }
            perspectiveWindow.getService(IEventBroker.class)
                    .unsubscribe(closeHandler);
        }
    }

    /**
     * Activate the perspective. This will call open if the perspective has not
     * been activated yet or has been closed, otherwise restores saved editors
     */
    public void activate() {
        if (perspectiveWindow.getActivePage() == null) {
            // don't attempt to load until there is an active page
            return;
        }

        page = perspectiveWindow.getActivePage();

        if (savedEditorAreaUI.isEmpty()) {
            savedEditorAreaUI.add(createDefaultEditorStack());
        }
        List<MPartSashContainerElement> oldEditors = swapEditorArea(
                savedEditorAreaUI);
        IPresentationEngine presentationEngine = perspectiveWindow
                .getService(IPresentationEngine.class);
        for (MPartSashContainerElement element : oldEditors) {
            presentationEngine.removeGui(element);
        }
        savedEditorAreaUI.clear();

        if (!opened) {
            backgroundColor = BackgroundColor
                    .getInstance(page.getPerspective());
            backgroundColor.addListener(BGColorMode.GLOBAL, this);
            open();
            perspectiveWindow.getService(IEventBroker.class)
                    .subscribe(UIEvents.UIElement.TOPIC_WIDGET, closeHandler);
            opened = true;
        } else {
            activateInternal();
        }
        activateContexts();
        for (AbstractModalTool tool : toolManager.getSelectedModalTools()) {
            tool.activate();
        }
        contributeToStatusLine();

        perspectiveWindow.getShell().setText(getTitle(title));
    }

    /**
     * Create an editor stack for the compatibility layer, based off of code in
     * org.eclipse.e4.ui.internal.workbench.PlaceholderResolver and
     * org.eclipse.ui.internal.e4.compatibility.ModeledPageLayout.
     */
    private MPartStack createDefaultEditorStack() {
        EModelService modelService = perspectiveWindow
                .getService(EModelService.class);
        MPartStack editorStack = modelService
                .createModelElement(MPartStack.class);
        editorStack.getTags().add("org.eclipse.e4.primaryDataStack");
        editorStack.getTags().add("EditorStack");
        editorStack.setElementId("org.eclipse.e4.primaryDataStack");
        return editorStack;
    }

    /**
     * Remove the elements currently in the editor area and replace them with
     * the provided elements. The old elements are returned and must be either
     * saved off or removed from the UI by the caller.
     * 
     * This method is needed to work around
     * https://bugs.eclipse.org/bugs/show_bug.cgi?id=374132. This method must
     * account for many subtle interactions between the model, renderer, addons,
     * and compatibility layer to ensure that all parts of the system remain
     * consistent.
     * 
     * 
     * @param newContents
     *            the contents that should go in the editor area.
     * @return the elements that were previously in the editor area.
     */
    private List<MPartSashContainerElement> swapEditorArea(
            List<? extends MPartSashContainerElement> newContents) {

        MWindow window = perspectiveWindow.getService(MWindow.class);
        EModelService modelService = perspectiveWindow
                .getService(EModelService.class);
        EPartService partService = perspectiveWindow
                .getService(EPartService.class);
        MPerspective perspective = modelService.getActivePerspective(window);
        List<MPlaceholder> editorPlaceholders = modelService.findElements(
                perspective, IPageLayout.ID_EDITOR_AREA, MPlaceholder.class,
                null);

        if (editorPlaceholders.size() == 1) {
            MPlaceholder editorPlaceholder = editorPlaceholders.get(0);
            MUIElement editorElement = editorPlaceholder.getRef();
            if (editorElement instanceof MArea) {
                MArea editorArea = (MArea) editorElement;
                List<MPartSashContainerElement> children = editorArea
                        .getChildren();

                /*
                 * The compatibility layer, specifically the WorkbenchPage, does
                 * not send out notifications when the model is changed like
                 * this. Specifically this causes problems with parts being
                 * hidden or deactivated because listeners may believe the parts
                 * we are hiding are still active/visible. To force the
                 * compatibility layer to send events we create blank MParts and
                 * make them the selected elements for all MPartStacks. The
                 * compatibility layer sees that these blank MParts are covering
                 * the currently visible parts and sends the correct events.
                 */
                MPart activePart = partService.getActivePart();
                List<MPartStack> stacks = modelService.findElements(editorArea,
                        null, MPartStack.class, null);
                for (MPartStack stack : stacks) {
                    if (stack.getChildren().isEmpty()) {
                        continue;
                    }
                    MPart cover = modelService.createModelElement(MPart.class);
                    cover.setElementId("cover");
                    cover.setLabel("Cover");
                    stack.getChildren().add(cover);
                    stack.setSelectedElement(cover);
                    if (stack.getChildren().contains(activePart)) {
                        partService.activate(cover);
                    }
                }

                /*
                 * When the editor area contains no visible children then the
                 * CleanupAddon sets the visibility to false, if that happens
                 * there is no way to restore visibility(see
                 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=365902). Adding
                 * HIDDEN_EXPLICITLY forces the CleanupAddon to ignore the
                 * editorArea which avoids that mess.
                 */
                List<String> tags = editorArea.getTags();
                tags.add(IPresentationEngine.HIDDEN_EXPLICITLY);
                List<MPartSashContainerElement> oldContents = new ArrayList<MPartSashContainerElement>(
                        children);
                for (MPartSashContainerElement element : oldContents) {
                    /*
                     * If the element is not set invisible then the renderer
                     * does not seem to notice it is gone and will leave it in
                     * the layout.
                     */
                    element.setVisible(false);
                    children.remove(element);
                }
                for (MPartSashContainerElement element : newContents) {
                    children.add(element);
                    element.setVisible(true);
                }
                tags.remove(IPresentationEngine.HIDDEN_EXPLICITLY);

                List<MPart> covers = modelService.findElements(editorArea,
                        "cover", MPart.class, null);
                for (MPart cover : covers) {
                    cover.getParent().getChildren().remove(cover);
                }

                return oldContents;
            } else {
                statusHandler.warn(
                        "Unable to find editor area, cannot swap editor area.");
            }
        } else if (editorPlaceholders.isEmpty()) {
            statusHandler.warn(
                    "Unable to find editor placeholder, cannot swap editor area.");
        } else {
            statusHandler.warn(
                    "Too many editor placeholders found, cannot swap editor area.");
        }
        return Collections.emptyList();
    }

    /**
     * Overridable set title method, takes original window title and
     * perspectives can display what they want. Default implementation sets
     * title to be "title - perspective"
     * 
     * @param title
     */
    protected String getTitle(String title) {
        return title + " - " + getLabel();
    }

    /**
     * Get the label for the perspective
     * 
     * @return
     */
    protected final String getLabel() {
        return page.getPerspective().getLabel();
    }

    protected void activateInternal() {
        if (activeEditor != null) {
            page.activate(activeEditor);
            activeEditor = null;
        }

        // Activate any perspective dialogs
        activateDialogs();
    }

    /**
     * Deactivate the perspective, stores editors to be opened again
     */
    public void deactivate() {

        activeEditor = page.getActiveEditor();

        savedEditorAreaUI.addAll(
                swapEditorArea(Arrays.asList(createDefaultEditorStack())));

        MWindow window = perspectiveWindow.getService(MWindow.class);
        EModelService modelService = perspectiveWindow
                .getService(EModelService.class);
        MPerspective perspective = (MPerspective) modelService
                .find(perspectiveId, window);
        if (perspective.getTags().contains("PerspClosing")) {
            IPresentationEngine presentation = perspectiveWindow
                    .getService(IPresentationEngine.class);
            for (MUIElement saved : savedEditorAreaUI) {
                presentation.removeGui(saved);
            }
            savedEditorAreaUI.clear();
        }

        deactivateDialogs();
        for (AbstractModalTool tool : toolManager.getSelectedModalTools()) {
            tool.deactivate();
        }
        deactivateContexts();
        removeFromStatusLine();
    }

    /**
     * Can be overridden to allow perspectives to override the editor's default
     * tool to have a default perspective tool
     * 
     * @param tool
     * @throws VizException
     */
    protected void activateDefaultTool(String tool) throws VizException {
        toolManager.activateToolSet(tool);
    }

    /**
     * WorkbenchWindow setter, should be called immediately after construction.
     * not passed in through constructor bc class will be instantiated through
     * eclipse extension point
     * 
     * @param window
     */
    public void setPerspectiveWindow(IWorkbenchWindow window) {
        this.perspectiveWindow = window;
        this.title = window.getShell().getText();
    }

    /**
     * Set the status line manager so the perspective can add to the status line
     * 
     * @param statusLine
     */
    public void setStatusLineManager(IStatusLineManager statusLine) {
        this.statusLine = statusLine;
    }

    /**
     * Set the perspective id registered with the manager in the plugin.xml
     * 
     * @param perspectiveId
     */
    void setPerspectiveId(String perspectiveId) {
        this.perspectiveId = perspectiveId;
    }

    public ModalToolManager getToolManager() {
        return toolManager;
    }

    /**
     * Add a perspective dialog
     * 
     * @param dialog
     */
    public void addPerspectiveDialog(IPerspectiveSpecificDialog dialog) {
        if (perspectiveDialogs.contains(dialog) == false) {
            perspectiveDialogs.add(dialog);
        }
    }

    /**
     * Remove a perspective dialog
     * 
     * @param dialog
     */
    public void removePespectiveDialog(IPerspectiveSpecificDialog dialog) {
        perspectiveDialogs.remove(dialog);
    }

    private void activateDialogs() {
        for (IPerspectiveSpecificDialog dialog : perspectiveDialogs) {
            dialog.restore(true);
        }
    }

    private void deactivateDialogs() {
        for (IPerspectiveSpecificDialog dialog : perspectiveDialogs) {
            dialog.hide(true);
        }
    }

    private void closeDialogs() {
        List<IPerspectiveSpecificDialog> dialogsToClose = new ArrayList<IPerspectiveSpecificDialog>();
        dialogsToClose.addAll(perspectiveDialogs);
        perspectiveDialogs.clear();
        for (IPerspectiveSpecificDialog dialog : dialogsToClose) {
            dialog.close();
        }
    }

    protected void loadDefaultBundle(String filePath) {
        LocalizationFile defaultBundle = PathManagerFactory.getPathManager()
                .getStaticLocalizationFile(filePath);
        try (InputStream is = defaultBundle.openInputStream()) {
            Procedure proc = (Procedure) ProcedureXmlManager.getInstance()
                    .unmarshal(is);
            LoadPerspectiveHandler.loadProcedureToScreen(proc, true);
        } catch (VizException | IOException | LocalizationException
                | SerializationException e) {
            statusHandler.handle(Priority.CRITICAL,
                    "Error activating perspective", e);
            return;
        }
    }

    public final void activateContexts() {
        activateContexts(ContextManager.getInstance(perspectiveWindow));
    }

    protected void activateContexts(ContextManager manager) {
        manager.activateContexts(this);
        if (page != null) {
            manager.activateContexts(page.getActivePart());
        }
    }

    public final void deactivateContexts() {
        deactivateContexts(ContextManager.getInstance(perspectiveWindow));
    }

    protected void deactivateContexts(ContextManager manager) {
        manager.deactivateContexts(this);
        if (page != null && page.getActivePart() != null
                && perspectiveId.equals(page.getPerspective().getId())) {
            manager.deactivateContexts(page.getActivePart());
        }
    }

    /**
     * Get an array of the editors in the perspective. Note: These editors may
     * not be visible and this perspective may not be the active perspective.
     * 
     * @return Array of available editors for the perspective
     */
    public static AbstractEditor[] getPerspectiveEditors() {
        List<AbstractEditor> editors = new ArrayList<AbstractEditor>();
        if (page != null) {
            for (IEditorReference ref : page.getEditorReferences()) {
                IEditorPart part = ref.getEditor(false);
                if (part instanceof AbstractEditor) {
                    editors.add((AbstractEditor) part);
                }
            }
        }
        return editors.toArray(new AbstractEditor[editors.size()]);
    }

    /**
     * Get the perspective id the manager manages
     * 
     * @return
     */
    public String getPerspectiveId() {
        return perspectiveId;
    }

    /**
     * Have the perspecitve manager open a new editor for the perspective
     * 
     * @return the new editor or null if no editor was opened
     */
    public AbstractEditor openNewEditor() {
        // default does nothing
        return null;
    }

    private void contributeToStatusLine() {
        items.addAll(getStatusLineItems());
        for (int i = items.size() - 1; i >= 0; --i) {
            IContributionItem item = items.get(i);
            item.setVisible(true);
            statusLine.appendToGroup(StatusLineManager.MIDDLE_GROUP, item);
        }
        statusLine.update(true);
        // relayout the shell since we added the widget
        perspectiveWindow.getShell().layout(true, true);
    }

    private void removeFromStatusLine() {
        for (IContributionItem item : items) {
            statusLine.remove(item);
            item.dispose();
        }
        statusLine.update(true);
        // relayout the shell since we added the widget
        perspectiveWindow.getShell().layout(true, true);
        items.clear();
    }

    /**
     * Adds perspective specific context menu items to the specified
     * IMenuManager on the IDisplayPaneContainer for the IDisplayPane
     * 
     * @param menuManager
     * @param container
     * @param pane
     */
    public void addContextMenuItems(IMenuManager menuManager,
            IDisplayPaneContainer container, IDisplayPane pane) {
        // do nothing default implementation
    }

    @Override
    public void setColor(BGColorMode mode, RGB newColor) {
        for (AbstractEditor editor : getPerspectiveEditors()) {
            editor.setColor(mode, newColor);
        }
    }

    /**
     * Notify perspective manager when heap space is running low. Default action
     * is to pop up a warning to the user. Perspectives can override the default
     * behavior to take more extreme actions to reduce memory usage.
     * 
     * @param freeMemory
     *            free memory available in bytes
     * @return true if notification was displayed
     */
    public boolean notifyLowMemory(long availMemory) {
        final String msg = getLowMemoryMessage(availMemory);
        final boolean[] status = new boolean[1];
        VizApp.runSync(new Runnable() {
            @Override
            public void run() {
                Display display = Display.getDefault();
                status[0] = MessageDialog.open(MessageDialog.WARNING,
                        display.getActiveShell(), "Low Memory", msg, SWT.NONE);
            }
        });
        return status[0];
    }

    /**
     * Create the default low memory message to be displayed to the user
     * 
     * @param availMemory
     *            free memory available in bytes
     * @return
     */
    protected String getLowMemoryMessage(long availMemory) {
        return "This CAVE is nearing its maximum memory limit. "
                + "Performance may degrade significantly. "
                + SizeUtil.prettyByteSize(availMemory) + " available";
    }

}
