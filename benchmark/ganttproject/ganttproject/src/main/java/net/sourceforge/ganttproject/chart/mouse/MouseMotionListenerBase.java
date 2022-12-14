/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.sourceforge.ganttproject.chart.mouse;

import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import net.sourceforge.ganttproject.AbstractChartImplementation;
import net.sourceforge.ganttproject.gui.UIFacade;

public class MouseMotionListenerBase extends MouseMotionAdapter {
  private UIFacade myUiFacade;
  private AbstractChartImplementation myChartImplementation;

  public MouseMotionListenerBase(UIFacade uiFacade, AbstractChartImplementation chartImplementation) {
    myUiFacade = uiFacade;
    myChartImplementation = chartImplementation;
  }

  protected UIFacade getUIFacade() {
    return myUiFacade;
  }

  @Override
  public void mouseDragged(MouseEvent e) {
    super.mouseDragged(e);
    MouseInteraction activeInteraction = myChartImplementation.getActiveInteraction();
    if (activeInteraction != null) {
      activeInteraction.apply(e);
      myChartImplementation.reset();
    }
  }
}