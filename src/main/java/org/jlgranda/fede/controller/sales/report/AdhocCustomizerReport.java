/**
 * DynamicReports - Free Java reporting library for creating reports dynamically
 *
 * Copyright (C) 2010 - 2016 Ricardo Mariaca
 * http://www.dynamicreports.org
 *
 * This file is part of DynamicReports.
 *
 * DynamicReports is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DynamicReports is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with DynamicReports. If not, see <http://www.gnu.org/licenses/>.
 */

package org.jlgranda.fede.controller.sales.report;

import java.math.BigDecimal;
import static net.sf.dynamicreports.report.builder.DynamicReports.*;

import java.util.Date;
import javax.faces.context.FacesContext;

import net.sf.dynamicreports.adhoc.AdhocManager;
import net.sf.dynamicreports.adhoc.configuration.AdhocCalculation;
import net.sf.dynamicreports.adhoc.configuration.AdhocColumn;
import net.sf.dynamicreports.adhoc.configuration.AdhocConfiguration;
import net.sf.dynamicreports.adhoc.configuration.AdhocGroup;
import net.sf.dynamicreports.adhoc.configuration.AdhocReport;
import net.sf.dynamicreports.adhoc.configuration.AdhocSort;
import net.sf.dynamicreports.adhoc.configuration.AdhocSubtotal;
import net.sf.dynamicreports.adhoc.report.DefaultAdhocReportCustomizer;
import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.jasper.builder.export.JasperPdfExporterBuilder;
import net.sf.dynamicreports.report.builder.ReportBuilder;
import net.sf.dynamicreports.report.datasource.DRDataSource;
import net.sf.dynamicreports.report.definition.datatype.DRIDataType;
import net.sf.dynamicreports.report.exception.DRException;
import net.sf.jasperreports.engine.JRDataSource;
import org.jlgranda.fede.model.sales.Detail;
import org.jlgranda.fede.model.sales.Invoice;

/**
 * @author Ricardo Mariaca (r.mariaca@dynamicreports.org)
 * @adaptedby José Luis Granda (jlgranda@gmail.com)
 */
public class AdhocCustomizerReport {

	public AdhocCustomizerReport(Invoice invoice) {
		build(invoice);
	}

	private void build(Invoice invoice) {
		AdhocConfiguration configuration = new AdhocConfiguration();
		AdhocReport report = new AdhocReport();
		configuration.setReport(report);

		//columns
		AdhocColumn column = new AdhocColumn();
		column.setName("cantidad");
		report.addColumn(column);
		column = new AdhocColumn();
		column.setName("descripcion");
		report.addColumn(column);
		column = new AdhocColumn();
		column.setName("preciounitario");
		report.addColumn(column);
		column = new AdhocColumn();
		column.setName("subtotal");
		report.addColumn(column);
//		//groups
//		AdhocGroup group = new AdhocGroup();
//		group.setName("invoice");
//		report.addGroup(group);
		//subtotal
//		AdhocSubtotal subtotal = new AdhocSubtotal();
//		subtotal.setName("quantity");
//		subtotal.setCalculation(AdhocCalculation.COUNT);
//		report.addSubtotal(subtotal);
		AdhocSubtotal subtotal = new AdhocSubtotal();
		subtotal.setCalculation(AdhocCalculation.SUM);
		subtotal.setName("subtotal");
		report.addSubtotal(subtotal);
//		//sorts
//		AdhocSort sort = new AdhocSort();
//		sort.setName("item");
//		report.addSort(sort);

	  try {
              
                        JasperPdfExporterBuilder pdfExporter = export.pdfExporter("/tmp/" + invoice.getSequencial() +".pdf")
	                                                      .setEncrypted(false);
			JasperReportBuilder reportBuilder = AdhocManager.createReport(configuration.getReport(), new ReportCustomizer());
			reportBuilder.setDataSource(createDataSource(invoice));
			reportBuilder.toPdf(pdfExporter);
		} catch (DRException e) {
			e.printStackTrace();
		}
	}

	private JRDataSource createDataSource(Invoice invoice) {
		DRDataSource dataSource = new DRDataSource("cantidad", "descripcion", "preciounitario", "subtotal");
                for (Detail detail : invoice.getDetails()){
                    dataSource.add(detail.getAmount(), detail.getProduct().getName(), detail.getPrice(), detail.getPrice().multiply(BigDecimal.valueOf(detail.getAmount())));
                }
		return dataSource;
	}

	public static void main(String[] args) {
		new AdhocCustomizerReport(new Invoice());
	}

	private class ReportCustomizer extends DefaultAdhocReportCustomizer {

		/**
		 * If you want to add some fixed content to a report that is not needed to store in the xml file.
		 * For example you can add default page header, footer, default fonts,...
		 */
		@Override
		public void customize(ReportBuilder<?> report, AdhocReport adhocReport) throws DRException {
			super.customize(report, adhocReport);
			//default report values
			report.setTemplate(Templates.reportTemplate);
			report.title(Templates.createTitleComponent("AdhocCustomizer"));
			//a fixed page footer that user cannot change, this customization is not stored in the xml file
			report.pageFooter(Templates.footerComponent);
		}

		/**
		 * This method returns a field type from a given field name.
		 */
		@Override
		protected DRIDataType<?, ?> getFieldType(String name) {
			
			if (name.equals("cantidad")) {
				return type.floatType();
			}
			if (name.equals("descripcion")) {
				return type.stringType();
			}
			if (name.equals("preciounitario")) {
				return type.bigDecimalType();
			}
			if (name.equals("subtotal")) {
				return type.bigDecimalType();
			}
			return super.getFieldType(name);
		}

		/**
		 * If a user doesn’t specify a column title, getColumnTitle is evaluated and the return value is used as a column title.
		 */
		@Override
		protected String getFieldLabel(String name) {
			if (name.equals("invoice")) {
				return "Factura";
			}
			if (name.equals("descripcion")) {
				return "Detalle";
			}
			if (name.equals("cantidad")) {
				return "Cant.";
			}
			if (name.equals("preciounitario")) {
				return "Precio Unitario";
			}
			if (name.equals("subtotal")) {
				return "Subtotal";
			}
			return name;
		}

	}
}