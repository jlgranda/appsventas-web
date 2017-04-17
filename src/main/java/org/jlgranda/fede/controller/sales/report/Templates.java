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

import static net.sf.dynamicreports.report.builder.DynamicReports.*;

import java.awt.Color;
import java.util.Locale;

import net.sf.dynamicreports.report.base.expression.AbstractValueFormatter;
import net.sf.dynamicreports.report.builder.HyperLinkBuilder;
import net.sf.dynamicreports.report.builder.ReportTemplateBuilder;
import net.sf.dynamicreports.report.builder.component.ComponentBuilder;
import net.sf.dynamicreports.report.builder.datatype.BigDecimalType;
import net.sf.dynamicreports.report.builder.style.FontBuilder;
import net.sf.dynamicreports.report.builder.style.SimpleStyleBuilder;
import net.sf.dynamicreports.report.builder.style.StyleBuilder;
import net.sf.dynamicreports.report.builder.tableofcontents.TableOfContentsCustomizerBuilder;
import net.sf.dynamicreports.report.constant.HorizontalTextAlignment;
import net.sf.dynamicreports.report.constant.PageType;
import net.sf.dynamicreports.report.constant.VerticalTextAlignment;
import net.sf.dynamicreports.report.definition.ReportParameters;
import org.jlgranda.fede.model.sales.Invoice;

/**
 * @author Ricardo Mariaca (r.mariaca@dynamicreports.org)
 */
public class Templates {
	public static final StyleBuilder rootStyle;
	public static final StyleBuilder boldStyle;
	public static final StyleBuilder italicStyle;
	public static final StyleBuilder boldCenteredStyle;
	public static final StyleBuilder bold10CenteredStyle;
	public static final StyleBuilder bold12CenteredStyle;
	public static final StyleBuilder bold18CenteredStyle;
	public static final StyleBuilder bold22CenteredStyle;
	public static final StyleBuilder columnStyle;
	public static final StyleBuilder columnTitleStyle;
	public static final StyleBuilder groupStyle;
	public static final StyleBuilder subtotalStyle;
	public static final StyleBuilder detailHeaderStyle;
        public static final SimpleStyleBuilder evenStyle;
        public static final SimpleStyleBuilder oddStyle;

	public static final ReportTemplateBuilder reportTemplate;
	public static final CurrencyType currencyType;
	public static final ComponentBuilder<?, ?> dynamicReportsComponent;
	public static final ComponentBuilder<?, ?> jlgrandaComponent;
	public static final ComponentBuilder<?, ?> footerComponent;

	static {
		rootStyle           = stl.style().setPadding(2).setFontName("BM receipt");
		boldStyle           = stl.style(rootStyle).bold();
		italicStyle         = stl.style(rootStyle).italic();
		boldCenteredStyle   = stl.style(boldStyle)
		                         .setTextAlignment(HorizontalTextAlignment.CENTER, VerticalTextAlignment.MIDDLE);
		bold10CenteredStyle = stl.style(boldCenteredStyle)
		                         .setFontSize(10);
		bold12CenteredStyle = stl.style(boldCenteredStyle)
		                         .setFontSize(12);
		bold18CenteredStyle = stl.style(boldCenteredStyle)
		                         .setFontSize(18);
		bold22CenteredStyle = stl.style(boldCenteredStyle)
                             .setFontSize(22);
		columnStyle         = stl.style(rootStyle).setVerticalTextAlignment(VerticalTextAlignment.MIDDLE);
		columnTitleStyle    = stl.style(columnStyle)
		                         .setBorder(stl.pen1Point())
		                         .setHorizontalTextAlignment(HorizontalTextAlignment.CENTER)
		                         .setBackgroundColor(Color.LIGHT_GRAY)
		                         .bold();
                detailHeaderStyle    = stl.style(columnStyle)
		                         .setBorder(stl.pen1Point())
		                         .setHorizontalTextAlignment(HorizontalTextAlignment.CENTER)
		                         .setBackgroundColor(Color.WHITE)
                                         .setForegroundColor(Color.WHITE)
		                         .bold();
		groupStyle          = stl.style(boldStyle)
		                         .setHorizontalTextAlignment(HorizontalTextAlignment.LEFT);
		subtotalStyle       = stl.style(boldStyle)
		                         .setTopBorder(stl.pen1Point());
                evenStyle = stl.simpleStyle().setBackgroundColor(Color.WHITE);
                oddStyle = stl.simpleStyle().setBackgroundColor(Color.WHITE);

		StyleBuilder crosstabGroupStyle      = stl.style(columnTitleStyle);
		StyleBuilder crosstabGroupTotalStyle = stl.style(columnTitleStyle)
		                                          .setBackgroundColor(new Color(170, 170, 170));
		StyleBuilder crosstabGrandTotalStyle = stl.style(columnTitleStyle)
		                                          .setBackgroundColor(new Color(140, 140, 140));
		StyleBuilder crosstabCellStyle       = stl.style(columnStyle)
		                                          .setBorder(stl.pen1Point());
                
		TableOfContentsCustomizerBuilder tableOfContentsCustomizer = tableOfContentsCustomizer()
			.setHeadingStyle(0, stl.style(rootStyle).bold());

		reportTemplate = template()
                                    .setPageFormat(PageType.A6)
                                    .setPageMargin(margin(20))
		                   .setLocale(Locale.ENGLISH)
		                   .setColumnStyle(columnStyle)
		                   .setColumnTitleStyle(columnTitleStyle)
		                   .setGroupStyle(groupStyle)
		                   .setGroupTitleStyle(groupStyle)
		                   .setSubtotalStyle(subtotalStyle)
		                   .highlightDetailEvenRows()
		                   .crosstabHighlightEvenRows()
		                   .setCrosstabGroupStyle(crosstabGroupStyle)
		                   .setCrosstabGroupTotalStyle(crosstabGroupTotalStyle)
		                   .setCrosstabGrandTotalStyle(crosstabGrandTotalStyle)
		                   .setCrosstabCellStyle(crosstabCellStyle)
		                   .setTableOfContentsCustomizer(tableOfContentsCustomizer)
                                   .setDetailHeaderStyle(detailHeaderStyle)
                                   .setDetailEvenRowStyle(evenStyle)
                                   .setDetailOddRowStyle(oddStyle);

		currencyType = new CurrencyType();

		HyperLinkBuilder link = hyperLink("http://www.jlgranda.com");
		dynamicReportsComponent =
		  cmp.horizontalList(
		  	cmp.image(Templates.class.getResource("images/appsventas.png")).setFixedDimension(15, 15),
		  	cmp.verticalList(
		  		cmp.text("jlgranda.com").setStyle(bold22CenteredStyle).setHorizontalTextAlignment(HorizontalTextAlignment.LEFT),
		  		cmp.text("http://www.jlgranda.com").setStyle(italicStyle).setHyperLink(link))).setFixedWidth(200);
		jlgrandaComponent =
		  cmp.horizontalList(
		  	cmp.verticalList(
		  		cmp.text("jlgranda.com, consigue más con la tecnología!").setStyle(rootStyle).setHorizontalTextAlignment(HorizontalTextAlignment.CENTER)));

		footerComponent = cmp.pageXofY()
		                     .setStyle(
		                     	stl.style(boldCenteredStyle)
		                     	   .setTopBorder(stl.pen1Point()));
	}

	/**
	 * Creates custom component which is possible to add to any report band component
	 */
	public static ComponentBuilder<?, ?> createTitleComponent(String label) {
		return cmp.horizontalList()
		        .add(
		        	dynamicReportsComponent,
		        	cmp.text(label).setStyle(bold12CenteredStyle).setHorizontalTextAlignment(HorizontalTextAlignment.RIGHT))
		        .newRow()
		        .add(cmp.line())
		        .newRow()
		        .add(cmp.verticalGap(10));
	}
        
	/**
	 * Creates custom component which is possible to add to any report band component
	 */
	public static ComponentBuilder<?, ?> createInvoiceHeaderComponent(Invoice invoice) {
		return cmp.horizontalList()
		        .newRow()
		        .add(cmp.verticalGap(120))
                        .newRow()
                        .add(cmp.gap(85, 13))
                        .add(cmp.text(invoice.getCreatedOn()).setStyle(rootStyle).setHorizontalTextAlignment(HorizontalTextAlignment.LEFT))
		        .newRow()
                        .add(cmp.gap(45, 13))
                        .add(cmp.text(invoice.getOwner().getFullName()).setStyle(rootStyle).setHorizontalTextAlignment(HorizontalTextAlignment.LEFT))
		        .newRow()
                        .add(cmp.gap(60, 13))
                        .add(cmp.text(invoice.getOwner().getDescription()).setStyle(rootStyle).setHorizontalTextAlignment(HorizontalTextAlignment.LEFT))
                        .newRow()
                        .add(cmp.gap(48, 13))
                        .add(cmp.text(invoice.getOwner().getCode()).setStyle(rootStyle).setHorizontalTextAlignment(HorizontalTextAlignment.LEFT), 
                                cmp.gap(58, 13), 
                                cmp.text(invoice.getOwner().getMobileNumber()).setStyle(rootStyle).setHorizontalTextAlignment(HorizontalTextAlignment.RIGHT));
	}

	public static CurrencyValueFormatter createCurrencyValueFormatter(String label) {
		return new CurrencyValueFormatter(label);
	}

	public static class CurrencyType extends BigDecimalType {
		private static final long serialVersionUID = 1L;

		@Override
		public String getPattern() {
			return "$ #,###.00";
		}
	}

	private static class CurrencyValueFormatter extends AbstractValueFormatter<String, Number> {
		private static final long serialVersionUID = 1L;

		private String label;

		public CurrencyValueFormatter(String label) {
			this.label = label;
		}

		@Override
		public String format(Number value, ReportParameters reportParameters) {
			return label + currencyType.valueToString(value, reportParameters.getLocale());
		}
	}
}