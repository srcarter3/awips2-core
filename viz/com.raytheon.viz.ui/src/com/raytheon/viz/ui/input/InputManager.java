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

package com.raytheon.viz.ui.input;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import com.raytheon.uf.viz.core.IDisplayPaneContainer;
import com.raytheon.uf.viz.core.rsc.IContainerAwareInputHandler;
import com.raytheon.uf.viz.core.rsc.IInputHandler;
import com.raytheon.uf.viz.core.rsc.IInputHandler.InputPriority;
import com.raytheon.uf.viz.core.rsc.IInputHandler2;

/**
 * Manage the {@link IInputHandler}s that are registered for an
 * {@link IDisplayPaneContainer}.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jul 01, 2006           chammack  Initial Creation.
 * Sep 11, 2014  3549     mschenke  Added mouse move notification after up
 * Jun 23, 2016  5674     randerso  Extend IInputHandler to pass raw event to
 *                                  handler
 * Aug 08, 2016  2676     bsteffen  Add IContainerAwareInputHandler
 * 
 * </pre>
 * 
 * @author chammack
 */
public class InputManager implements Listener {

    private class PrioritizedHandler implements Comparable<PrioritizedHandler> {
        InputPriority priority;

        IInputHandler handler;

        public PrioritizedHandler(IInputHandler handler, InputPriority priority) {
            this.handler = handler;
            this.priority = priority;
        }

        @Override
        public int compareTo(PrioritizedHandler o) {
            return priority.value.compareTo(o.priority.value);
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof IInputHandler) {
                return o == handler;
            }
            if (o instanceof PrioritizedHandler) {
                return ((PrioritizedHandler) o).handler == this.handler;
            }
            return false;
        }
    }

    private boolean isMouseDown = false;

    private boolean menuDetected = false;

    private final List<PrioritizedHandler> handlers;

    private final List<PrioritizedHandler> perspectiveHandlers;

    private int lastMouseButton;

    private final IDisplayPaneContainer container;

    /**
     * Constructor
     * 
     * @param container
     */
    public InputManager(IDisplayPaneContainer container) {
        this.handlers = new ArrayList<PrioritizedHandler>();
        this.perspectiveHandlers = new ArrayList<PrioritizedHandler>();
        this.container = container;
    }

    /**
     * Get all input handlers registered at a particular priority
     * 
     * @param priority
     * @return array of handlers for the specified priority
     */
    public IInputHandler[] getHandlersForPriority(InputPriority priority) {
        List<IInputHandler> handlers = new ArrayList<IInputHandler>();
        for (PrioritizedHandler handler : this.handlers) {
            if (handler.priority == priority) {
                handlers.add(handler.handler);
            }
        }
        return handlers.toArray(new IInputHandler[handlers.size()]);
    }

    /**
     * Handle Mouse Click events
     */
    @Override
    public void handleEvent(Event event) {

        if ((container == null)
                || (container.getActiveDisplayPane() == null)
                || (event.display != container.getActiveDisplayPane()
                        .getDisplay())) {
            return;
        }

        switch (event.type) {
        case SWT.MouseDown:
            handleMouseDown(event);
            break;
        case SWT.MouseUp:
            handleMouseUp(event);
            break;
        case SWT.MouseWheel:
            handleMouseWheel(event);
            break;
        case SWT.MouseMove:
            handleMouseMove(event);
            break;
        case SWT.MouseHover:
            handleMouseHover(event);
            break;
        case SWT.MouseDoubleClick:
            handleMouseDoubleClick(event);
            break;
        case SWT.KeyDown:
            handleKeyDown(event);
            break;
        case SWT.KeyUp:
            handleKeyUp(event);
            break;
        case SWT.MenuDetect:
            isMouseDown = false;
            menuDetected = true;
            break;
        case SWT.MouseExit: {
            handleMouseExit(event);
            break;
        }
        case SWT.MouseEnter: {
            handleMouseEnter(event);
            break;
        }
        default:
            break;
        }
    }

    private void handleMouseEnter(Event event) {
        for (int i = handlers.size() - 1; i >= 0; i--) {
            // Let all handlers know of event...
            handlers.get(i).handler.handleMouseEnter(event);
        }
    }

    private void handleMouseExit(Event event) {
        for (int i = handlers.size() - 1; i >= 0; i--) {
            // Let all handlers know of event...
            handlers.get(i).handler.handleMouseExit(event);
        }
    }

    private void handleMouseDoubleClick(Event e) {
        isMouseDown = false;
        for (int i = handlers.size() - 1; i >= 0; i--) {
            IInputHandler handler = handlers.get(i).handler;

            boolean status;
            if (handler instanceof IInputHandler2) {
                status = ((IInputHandler2) handler).handleDoubleClick(e);
            } else {
                status = handler.handleDoubleClick(e.x, e.y, e.button);
            }

            if (status) {
                break;
            }
        }
    }

    private void handleMouseUp(Event e) {
        isMouseDown = false;

        for (int i = handlers.size() - 1; i >= 0; i--) {
            IInputHandler handler = handlers.get(i).handler;

            boolean status;
            if (handler instanceof IInputHandler2) {
                status = ((IInputHandler2) handler).handleMouseUp(e);
            } else {
                status = handler.handleMouseUp(e.x, e.y, e.button);
            }

            if (status) {
                break;
            }
        }

        /*
         * On up, fire move so any positions can be refreshed for people who
         * were not getting notified for the mouse down/move/up and need to know
         * the current position
         */
        handleMouseMove(e);
    }

    private void handleMouseDown(Event e) {
        if (menuDetected && (e.button != 3)) {
            menuDetected = false;
            return;
        }

        if (e.type == SWT.MouseDoubleClick) {
            return;
        }

        lastMouseButton = e.button;

        if (!menuDetected) {
            isMouseDown = true;
        } else {
            menuDetected = false;
        }

        for (int i = handlers.size() - 1; i >= 0; i--) {
            IInputHandler handler = handlers.get(i).handler;

            boolean status;
            if (handler instanceof IInputHandler2) {
                status = ((IInputHandler2) handler).handleMouseDown(e);
            } else {
                status = handler.handleMouseDown(e.x, e.y, e.button);
            }

            if (status) {
                break;
            }
        }

    }

    private void handleMouseWheel(Event e) {
        for (int i = handlers.size() - 1; i >= 0; i--) {
            IInputHandler handler = handlers.get(i).handler;

            boolean status;
            if (handler instanceof IInputHandler2) {
                status = ((IInputHandler2) handler).handleMouseWheel(e);
            } else {
                status = handler.handleMouseWheel(e, e.x, e.y);
            }

            if (status) {
                break;
            }
        }
    }

    private void handleMouseHover(final Event e) {
        for (int i = handlers.size() - 1; i >= 0; i--) {
            IInputHandler handler = handlers.get(i).handler;

            boolean status;
            if (handler instanceof IInputHandler2) {
                status = ((IInputHandler2) handler).handleMouseHover(e);
            } else {
                status = handler.handleMouseHover(e.x, e.y);
            }

            if (status) {
                break;
            }
        }
    }

    private void handleKeyDown(final Event e) {
        for (int i = handlers.size() - 1; i >= 0; i--) {
            IInputHandler handler = handlers.get(i).handler;

            boolean status;
            if (handler instanceof IInputHandler2) {
                status = ((IInputHandler2) handler).handleKeyDown(e);
            } else {
                status = handler.handleKeyDown(e.keyCode);
            }

            if (status) {
                break;
            }
        }
    }

    private void handleKeyUp(final Event e) {
        for (int i = handlers.size() - 1; i >= 0; i--) {
            IInputHandler handler = handlers.get(i).handler;

            boolean status;
            if (handler instanceof IInputHandler2) {
                status = ((IInputHandler2) handler).handleKeyUp(e);
            } else {
                status = handler.handleKeyUp(e.keyCode);
            }

            if (status) {
                break;
            }
        }
    }

    /**
     * Mouse Move event
     */
    private void handleMouseMove(Event e) {

        if (isMouseDown) {
            for (int i = handlers.size() - 1; i >= 0; i--) {
                IInputHandler handler = handlers.get(i).handler;

                boolean status;
                if (handler instanceof IInputHandler2) {
                    status = ((IInputHandler2) handler).handleMouseDownMove(e);
                } else {
                    status = handler.handleMouseDownMove(e.x, e.y,
                            lastMouseButton);
                }

                if (status) {
                    break;
                }
            }
        } else {
            for (int i = 0; i < handlers.size(); i++) {
                // Let all handlers know about moves
                IInputHandler handler = handlers.get(i).handler;

                if (handler instanceof IInputHandler2) {
                    ((IInputHandler2) handler).handleMouseMove(e);
                } else {
                    handler.handleMouseMove(e.x, e.y);
                }
            }
        }
    }

    /**
     * Register a mouse handler, lowest priority are handled last
     * 
     * @param aHandler
     * @param priority
     * 
     */
    public void registerMouseHandler(IInputHandler aHandler,
            InputPriority priority) {
        if (aHandler instanceof IContainerAwareInputHandler) {
            ((IContainerAwareInputHandler) aHandler).setContainer(container);
        }
        PrioritizedHandler pHandler = new PrioritizedHandler(aHandler, priority);
        synchronized (this) {
            if (!handlers.contains(pHandler)) {
                handlers.add(pHandler);
            }
            Collections.sort(handlers);
        }
    }

    /**
     * Register a mouse handler
     * 
     * @param aHandler
     */
    public void registerMouseHandler(IInputHandler aHandler) {
        registerMouseHandler(aHandler, InputPriority.RESOURCE);
    }

    /**
     * Unregister a mouse handler
     * 
     * @param aHandler
     */
    public void unregisterMouseHandler(IInputHandler aHandler) {
        PrioritizedHandler pHandler = new PrioritizedHandler(aHandler,
                InputPriority.RESOURCE);
        synchronized (this) {
            handlers.remove(pHandler);
        }
    }

    /**
     * Notify the manager that the perspective has changed and the
     * perspective-specific handlers should be changed out
     * 
     * @param newPerspectiveSpecificHandlers
     *            a new set of perspective specific handlers
     */
    public void firePerspectiveChanged(
            IInputHandler[] newPerspectiveSpecificHandlers) {
        synchronized (this) {
            List<IInputHandler> newPerspectiveHandlerList = Arrays
                    .asList(newPerspectiveSpecificHandlers);
            // The perspective has changed

            // First, remove from the handler list the old perspective specific
            // handlers
            this.handlers.removeAll(this.perspectiveHandlers);

            // Then, clear the old perspective handlers
            this.perspectiveHandlers.clear();

            for (IInputHandler handler : newPerspectiveHandlerList) {
                PrioritizedHandler pHandler = new PrioritizedHandler(handler,
                        InputPriority.fromValue(0));
                // And add the new perspective handlers
                this.perspectiveHandlers.add(pHandler);

                // Then add to the master handler list
                this.handlers.add(pHandler);
            }

        }
        Collections.sort(handlers);

    }

}
