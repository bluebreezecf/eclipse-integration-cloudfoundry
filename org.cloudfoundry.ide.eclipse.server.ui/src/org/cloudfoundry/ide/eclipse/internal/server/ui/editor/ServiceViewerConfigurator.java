/*******************************************************************************
 * Copyright (c) 2012 VMware, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMware, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.editor;

import org.cloudfoundry.client.lib.CloudService;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

/**
 * Configures the services viewer providers, sorters and columns. Must be called
 * before setting the viewer input.
 * 
 */
public class ServiceViewerConfigurator {

	private boolean addAutomaticViewerResizing;

	public ServiceViewerConfigurator() {
		this.addAutomaticViewerResizing = false;
	}

	enum ServiceViewColumn {
		Name(150), Type(100), Vendor(200);
		private int width;

		private ServiceViewColumn(int width) {
			this.width = width;
		}

		public int getWidth() {
			return width;
		}
	}

	public ServiceViewerConfigurator enableAutomaticViewerResizing() {
		addAutomaticViewerResizing = true;
		return this;
	}

	/**
	 * This must be called before setting the viewer input
	 * @param tableViewer
	 */
	public void configureViewer(final TableViewer tableViewer) {
		tableViewer.setContentProvider(new ApplicationsMasterPartContentProvider());
		tableViewer.setLabelProvider(new ServicesTreeLabelProvider(tableViewer));

		final Table table = tableViewer.getTable();
		table.setHeaderVisible(true);

		int columnIndex = 0;
		ServiceViewColumn[] columns = ServiceViewColumn.values();
		String[] columnProperties = new String[columns.length];
		TableColumn sortColumn = null;
		for (ServiceViewColumn column : columns) {
			columnProperties[columnIndex] = column.name();
			TableColumn tableColumn = new TableColumn(table, SWT.NONE, columnIndex++);
			tableColumn.setData(column);
			tableColumn.setText(column.name());
			tableColumn.setWidth(column.getWidth());
			if (column == ServiceViewColumn.Name) {
				sortColumn = tableColumn;
			}

		}

		// Add a control listener to resize the columns such that there is no
		// empty space
		// after the last column
		if (addAutomaticViewerResizing) {
			table.getParent().addControlListener(new ControlAdapter() {
				public void controlResized(ControlEvent e) {
					Composite tableComposite = tableViewer.getTable().getParent();
					Rectangle tableCompositeArea = tableComposite.getClientArea();
					int width = tableCompositeArea.width;
					resizeTableColumns(width, table);
				}
			});
		}

		tableViewer.setColumnProperties(columnProperties);

		tableViewer.setSorter(new CloudFoundryViewerSorter() {

			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				TableColumn sortColumn = tableViewer.getTable().getSortColumn();
				if (sortColumn != null) {
					ServiceViewColumn serviceColumn = (ServiceViewColumn) sortColumn.getData();
					int result = 0;
					int sortDirection = tableViewer.getTable().getSortDirection();
					if (serviceColumn != null) {
						if (e1 instanceof CloudService && e2 instanceof CloudService) {
							CloudService service1 = (CloudService) e1;
							CloudService service2 = (CloudService) e2;
							switch (serviceColumn) {
							case Name:
								result = super.compare(viewer, e1, e2);
								break;
							case Type:
								result = service1.getType().compareTo(service2.getVendor());
								break;
							case Vendor:
								result = service1.getVendor().compareTo(service2.getVendor());
								break;
							}
						}
					}
					return sortDirection == SWT.UP ? result : -result;
				}

				return super.compare(viewer, e1, e2);
			}

		});

		if (sortColumn != null) {
			table.setSortColumn(sortColumn);
			table.setSortDirection(SWT.UP);
		}
	}

	protected void resizeTableColumns(int tableWidth, Table table) {
		TableColumn[] tableColumns = table.getColumns();

		if (tableColumns.length == 0) {
			return;
		}

		int total = 0;

		// resize only if there is empty space at the end of the table
		for (TableColumn column : tableColumns) {
			total += column.getWidth();
		}

		if (total < tableWidth) {
			// resize the last one
			TableColumn lastColumn = tableColumns[tableColumns.length - 1];
			int newWidth = (tableWidth - total) + lastColumn.getWidth();
			lastColumn.setWidth(newWidth);
		}

	}

}
