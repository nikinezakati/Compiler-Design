/*
Copyright 2003-2012 Dmitry Barashev, GanttProject Team

This file is part of GanttProject, an opensource project management tool.

GanttProject is free software: you can redistribute it and/or modify 
it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

GanttProject is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
 */
package biz.ganttproject.core.option;

public class DefaultDoubleOption extends GPAbstractOption<Double> implements DoubleOption {
  public DefaultDoubleOption(String id) {
    this(id, 0.0);
  }

  public DefaultDoubleOption(String id, Double initialValue) {
    super(id, initialValue);
  }

  @Override
  public String getPersistentValue() {
    return getValue() == null ? "" : getValue().toString();
  }

  @Override
  public void loadPersistentValue(String value) {
    try {
      resetValue(Double.parseDouble(value), true);
    } catch (NumberFormatException e) {
      resetValue(null, true);
    }
  }
}
