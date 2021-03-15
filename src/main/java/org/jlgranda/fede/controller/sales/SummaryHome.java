/*
 * Copyright (C) 2018 jlgranda
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jlgranda.fede.controller.sales;

import com.jlgranda.fede.ejb.FacturaElectronicaService;
import com.jlgranda.fede.ejb.sales.InvoiceService;
import com.jlgranda.fede.ejb.sales.ProductCache;
import com.jlgranda.fede.ejb.sales.ProductService;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.inject.Named;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import org.jlgranda.fede.controller.FacturaElectronicaHome;
import org.jlgranda.fede.controller.FedeController;
import org.jlgranda.fede.controller.SettingHome;
import org.jlgranda.fede.model.document.DocumentType;
import org.jlgranda.fede.model.document.EmissionType;
import org.jlgranda.fede.model.sales.Invoice;
import org.jlgranda.fede.model.sales.Payment;
import org.jlgranda.fede.model.sales.Product;
import org.jpapi.model.Group;
import org.jpapi.model.StatusType;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.Dates;
import org.jpapi.util.I18nUtil;
import org.jpapi.util.Strings;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.chart.Axis;
import org.primefaces.model.chart.AxisType;
import org.primefaces.model.chart.BarChartModel;
import org.primefaces.model.chart.CategoryAxis;
import org.primefaces.model.chart.ChartSeries;
import org.primefaces.model.chart.HorizontalBarChartModel;
import org.primefaces.model.chart.LineChartModel;
import org.primefaces.model.chart.LineChartSeries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jlgranda
 */
@ViewScoped
@Named
public class SummaryHome extends FedeController implements Serializable {

    private static final long serialVersionUID = 145155795064685887L;

    private static final int TICKANGLE = 30;

    Logger logger = LoggerFactory.getLogger(SummaryHome.class);

    @Inject
    private Subject subject;

    private Subject customer;

    @Inject
    private FacturaElectronicaHome facturaElectronicaHome;

    @EJB
    private FacturaElectronicaService facturaElectronicaService;

    @Inject
    private SettingHome settingHome;

    @EJB
    private ProductService productService;

    @EJB
    private ProductCache productCache;

    @EJB
    private InvoiceService invoiceService;
    //Calcular Resumen
    private BigDecimal grossSalesTotal;
    private BigDecimal discountTotal;
    private BigDecimal salesTotal;
    private BigDecimal purchaseTotal;
    private BigDecimal costTotal;
    private BigDecimal profilTotal;
    private Long paxTotal;
    private List<Object[]> listPurchases;
    private List<Object[]> listDiscount;

    /**
     * Selector de grupos de fechas
     */
    private Date grupoFechas;

    private BarChartModel barModelAmount;
    private BarChartModel barModelSales;
    private BarChartModel barModelCategory;
    private HorizontalBarChartModel horizontalProductsBarModel;
    private HorizontalBarChartModel horizontalPurchasesBarModel;
    private LineChartModel balanceLineChartModel;

    @PostConstruct
    private void init() {

        initializeDateInterval();

        setGrossSalesTotal(BigDecimal.ZERO);
        setDiscountTotal(BigDecimal.ZERO);
        setSalesTotal(BigDecimal.ZERO);
        setPurchaseTotal(BigDecimal.ZERO);
        setCostTotal(BigDecimal.ZERO);
        setProfilTotal(BigDecimal.ZERO);
        setPaxTotal(0L);

        setOutcome("dashboard");
        calculeSummary();

    }

    public BigDecimal getGrossSalesTotal() {
        return grossSalesTotal;
    }

    public void setGrossSalesTotal(BigDecimal grossSalesTotal) {
        this.grossSalesTotal = grossSalesTotal;
    }

    public BigDecimal getDiscountTotal() {
        return discountTotal;
    }

    public void setDiscountTotal(BigDecimal discountTotal) {
        this.discountTotal = discountTotal;
    }

    public BigDecimal getSalesTotal() {
        return salesTotal;
    }

    public void setSalesTotal(BigDecimal salesTotal) {
        this.salesTotal = salesTotal;
    }

    public BigDecimal getPurchaseTotal() {
        return purchaseTotal;
    }

    public void setPurchaseTotal(BigDecimal purchaseTotal) {
        this.purchaseTotal = purchaseTotal;
    }

    public BigDecimal getCostTotal() {
        return costTotal;
    }

    public void setCostTotal(BigDecimal costTotal) {
        this.costTotal = costTotal;
    }

    public BigDecimal getProfilTotal() {
        return profilTotal;
    }

    public void setProfilTotal(BigDecimal profilTotal) {
        this.profilTotal = profilTotal;
    }

    public Long getPaxTotal() {
        return paxTotal;
    }

    public void setPaxTotal(Long paxTotal) {
        this.paxTotal = paxTotal;
    }

    public Date getGrupoFechas() {
        return grupoFechas;
    }

    public void setGrupoFechas(Date grupoFechas) {
        this.grupoFechas = grupoFechas;
    }

    //  Listas de valores para vistas de información
    public List<Object[]> getListPurchases() {
        return listPurchases;
    }

    public void setListPurchases(List<Object[]> listPurchases) {
        this.listPurchases = listPurchases;
    }

    public BigDecimal getListPurchasesTotal() {
        BigDecimal total = new BigDecimal(0);
        for (int i = 0; i < getListPurchases().size(); i++) {
            total = total.add((BigDecimal) getListPurchases().get(i)[1]);
        }
        return total;
    }

    public List<Object[]> getListDiscount() {
        return listDiscount;
    }

    public List<Object[]> getListDiscount(Date _start, Date _end) {
        List<Object[]> objects = invoiceService.findObjectsByNamedQueryWithLimit("Invoice.findTotalInvoiceBussinesSalesDiscountBetween", Integer.MAX_VALUE, this.subject, DocumentType.INVOICE, StatusType.CLOSE.toString(), _start, _end, BigDecimal.ZERO);
        return objects;
    }

    public void setListDiscount(List<Object[]> listDiscount) {
        this.listDiscount = listDiscount;
    }

    public BigDecimal getListDiscountTotal() {
        BigDecimal total = new BigDecimal(0);
        for (int i = 0; i < getListDiscount().size(); i++) {
            total = total.add((BigDecimal) getListDiscount().get(i)[4]);
        }
        return total;
    }

    /**
     * Muestra
     */
    public void calculeSummary() {
        Date _start = Dates.minimumDate(getStart());
        Date _end = Dates.maximumDate(getEnd());
        calculeSummary(_start, _end);
        setListDiscount(getListDiscount(_start, _end));
    }

    public void calculeSummary(Date _start, Date _end) {

        this.costTotal = BigDecimal.ZERO;
        List<Object[]> objects = invoiceService.findObjectsByNamedQueryWithLimit("Invoice.findTotalInvoiceSalesDiscountBetween", Integer.MAX_VALUE, this.subject, DocumentType.INVOICE, StatusType.CLOSE.toString(), _start, _end);
        objects.stream().forEach((Object[] object) -> {
            this.grossSalesTotal = (BigDecimal) object[0];
            this.discountTotal = (BigDecimal) object[1];
            this.salesTotal = (BigDecimal) object[2];
        });
        objects = invoiceService.findObjectsByNamedQueryWithLimit("FacturaElectronica.findTotalByEmissionTypeBetween", Integer.MAX_VALUE, this.subject, _start, _end, EmissionType.PURCHASE_CASH);
        objects.stream().forEach((Object object) -> {
            this.purchaseTotal = (BigDecimal) object;
        });
        objects = invoiceService.findObjectsByNamedQueryWithLimit("Invoice.findTotalInvoiceSalesPaxBetween", Integer.MAX_VALUE, this.subject, DocumentType.INVOICE, StatusType.CLOSE.toString(), _start, _end);
        objects.stream().forEach((Object object) -> {
            this.paxTotal = (Long) object;
        });

        if (this.grossSalesTotal == null) {
            this.grossSalesTotal = BigDecimal.ZERO;
        }

        if (this.discountTotal == null) {
            this.discountTotal = BigDecimal.ZERO;
        }

        if (this.salesTotal == null) {
            this.salesTotal = BigDecimal.ZERO;
        }

        if (this.purchaseTotal == null) {
            this.purchaseTotal = BigDecimal.ZERO;
        }

        if (this.paxTotal == null) {
            this.paxTotal = 0L;
        }

        this.salesTotal = this.salesTotal.subtract(this.discountTotal);
        this.profilTotal = this.salesTotal.subtract(this.purchaseTotal.add(this.costTotal));

    }

    /**
     * Calcular el equivalente en porcentaje para la UI
     *
     * @return
     */
    public BigDecimal calculeProfitRate() {
        BigDecimal goal = BigDecimal.valueOf(Long.valueOf(settingHome.getValue("app.fede.sales.goal", "500")));
        this.refresh();
        BigDecimal profitRate = this.getProfilTotal().divide(goal);
        return profitRate;
    }

    public BigDecimal calculeProfitRateToday() {
        BigDecimal goal = BigDecimal.valueOf(Long.valueOf(settingHome.getValue("app.fede.sales.goal", "500")));
        clear();
        calculeSummary(Dates.minimumDate(this.getEnd()), Dates.maximumDate(this.getEnd()));
        BigDecimal profitRate = this.getProfilTotal().divide(goal);
        return profitRate;
    }

    public int calculeProfitRateEquivalentForUX() {
        BigDecimal bd = calculeProfitRate().multiply(BigDecimal.valueOf(100));
        bd.setScale(1, BigDecimal.ROUND_CEILING);
        return bd.intValue();
    }

    public BigDecimal countSalesToday() {
        Date _start = Dates.minimumDate(getStart());
        Date _end = Dates.maximumDate(getEnd());
        if (Dates.calculateNumberOfDaysBetween(_start, _end) <= 0) {
            int range = Integer.parseInt(settingHome.getValue("fede.dashboard.summary.range", "1"));
            _start = Dates.addDays(getStart(), -1 * range);
        }
        BigDecimal total = new BigDecimal(invoiceService.count("Invoice.countTotalInvoiceBetween", this.subject, DocumentType.INVOICE, StatusType.CLOSE.toString(), _start, _end));
        return total;
    }

    public BigDecimal calculeAverage(int days) {
        clear();
        Date yesterday = Dates.addDays(Dates.now(), -1);
        calculeSummary(Dates.minimumDate(Dates.addDays(yesterday, -1 * (days - 1))), Dates.maximumDate(yesterday));
        return getProfilTotal().divide(BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculeProfitRateAverage(int days) {
        BigDecimal goal = BigDecimal.valueOf(Long.valueOf(settingHome.getValue("app.fede.sales.goal", "500")));
        return calculeAverage(days).divide(goal);
    }

    @Override
    public void handleReturn(SelectEvent event) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Group getDefaultGroup() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Group> getGroups() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void clear() {
        setGrossSalesTotal(BigDecimal.ZERO);
        setDiscountTotal(BigDecimal.ZERO);
        setSalesTotal(BigDecimal.ZERO);
        setPurchaseTotal(BigDecimal.ZERO);
        setCostTotal(BigDecimal.ZERO);
        setProfilTotal(BigDecimal.ZERO);
    }

    /**
     * Refrescar la vista y calculos
     */
    public void refresh() {
        clear();
        calculeSummary();
    }

    /**
     * Refrescar para un grupo de fechas
     */
    public void refreshPorGrupoFechas() {
        clear();
        setStart(grupoFechas);
        setEnd(grupoFechas);
        calculeSummary();
    }

    public void clearBarModel() {
        setBarModelAmount(null);
        setBarModelSales(null);
        setBarModelCategory(null);
        setBalanceLineChartModel(null);
        setHorizontalProductsBarModel(null);
        setHorizontalPurchasesBarModel(null);
    }

    /*
        Getter and Setter de BarModels
     */
    public BarChartModel getBarModelAmount() {
        if (barModelAmount == null) {
            setBarModelAmount(createBarModelAmount());
        }
        return barModelAmount;
    }

    public void setBarModelAmount(BarChartModel barModelAmount) {
        this.barModelAmount = barModelAmount;
    }

    public BarChartModel getBarModelSales() {
        if (barModelSales == null) {
            setBarModelSales(createbarModelSales());
        }
        return barModelSales;
    }

    public void setBarModelSales(BarChartModel barModelSales) {
        this.barModelSales = barModelSales;
    }

    public BarChartModel getBarModelCategory() {
        if (barModelCategory == null) {
            setBarModelCategory(createbarModelCategory());
        }
        return barModelCategory;
    }

    public void setBarModelCategory(BarChartModel barModelCategory) {
        this.barModelCategory = barModelCategory;
    }

    public HorizontalBarChartModel getHorizontalProductsBarModel() {
        if (horizontalProductsBarModel == null) {
            this.horizontalProductsBarModel = createHorizontalBarModel();
        }
        return horizontalProductsBarModel;
    }

    public void setHorizontalProductsBarModel(HorizontalBarChartModel horizontalProductsBarModel) {
        this.horizontalProductsBarModel = horizontalProductsBarModel;
    }

    public HorizontalBarChartModel getHorizontalPurchasesBarModel() {
        if (horizontalPurchasesBarModel == null) {
            setHorizontalPurchasesBarModel(createHorizontalPurchasesBarModel());
        }
        return horizontalPurchasesBarModel;
    }

    public void setHorizontalPurchasesBarModel(HorizontalBarChartModel horizontalPurchasesBarModel) {
        this.horizontalPurchasesBarModel = horizontalPurchasesBarModel;
    }

    public LineChartModel getBalanceLineChartModel() {
        if (balanceLineChartModel == null) {
            setBalanceLineChartModel(createLineChartModel());
        }
        return balanceLineChartModel;
    }

    public void setBalanceLineChartModel(LineChartModel balanceLineChartModel) {
        this.balanceLineChartModel = balanceLineChartModel;
    }

    // BarModels
    private ChartSeries createProductsSeries(String label, String queryNamed) {
        ChartSeries chartSerie = new ChartSeries();
        chartSerie.setLabel(label);

        int top = Integer.valueOf(settingHome.getValue("app.fede.inventory.top", "10"));
        List<Object[]> objects = productService.findObjectsByNamedQueryWithLimit(queryNamed, top, getStart(), getEnd());

        objects.stream().forEach((Object[] object) -> {
            Product _product = productCache.lookup((Long) object[0]);
            if (_product != null) {
                _product.getStatistics().setCount((Double) object[1]);
                chartSerie.set(_product.getName(), _product.getStatistics().getCount());
            }
        });

        return chartSerie;
    }

    private BarChartModel createBarModelAmount() {
        BarChartModel model = new BarChartModel();
        ChartSeries product = createProductsSeries(I18nUtil.getMessages("app.fede.barchart.sales.label.amount"), "Product.findTopProductIdsBetween");

        model.addSeries(product);
        model.setTitle(I18nUtil.getMessages("app.fede.barchart.sales.date.a") + Dates.toString(getStart(), settingHome.getValue("fede.name.pattern", "dd/MM/yyyy"))
                + " " + I18nUtil.getMessages("app.fede.barchart.sales.date.b") + Dates.toString(getEnd(), "dd/MM/yyyy"));
        model.setLegendPosition(settingHome.getValue("app.fede.barchart.sales.legendPosition", "e"));
        model.setStacked(false);
        model.setAnimate(true);
        model.setShowPointLabels(false);
        model.setExtender("skinBar");

        Axis xAxis = new CategoryAxis(I18nUtil.getMessages("app.fede.barchart.sales.axis"));
        xAxis.setTickAngle(SummaryHome.TICKANGLE);
        model.getAxes().put(AxisType.X, xAxis);
        Axis yAxis = model.getAxis(AxisType.Y);
        yAxis.setLabel(I18nUtil.getMessages("app.fede.barchart.sales.amount.axis"));
        yAxis.setMin(Integer.valueOf(settingHome.getValue("app.fede.barchart.sales.min", "0")));
        double scale;
        if (!product.getData().isEmpty()) {
            scale = (double) product.getData().values().toArray()[0] * Double.valueOf(settingHome.getValue("app.fede.barchart.sales.multiply.scale", "1.2"));
            yAxis.setMax(scale);
        }

        return model;
    }

    private BarChartModel createbarModelSales() {
        BarChartModel model = new BarChartModel();
        ChartSeries product = createProductsSeries(I18nUtil.getMessages("app.fede.barchart.sales.label.prices"), "Product.findTopProductIdsBetweenPrice");

        model.addSeries(product);
        model.setTitle(I18nUtil.getMessages("app.fede.barchart.sales.date.a") + Dates.toString(getStart(), settingHome.getValue("fede.name.pattern", "dd/MM/yyyy")) + " " + I18nUtil.getMessages("app.fede.barchart.sales.date.b") + Dates.toString(getEnd(), "dd/MM/yyyy"));
        model.setLegendPosition(settingHome.getValue("app.fede.barchart.sales.legendPosition", "e"));
        model.setStacked(false);
        model.setAnimate(true);
        model.setShowPointLabels(false);
        model.setExtender("skinBar2");

        Axis xAxis = new CategoryAxis(I18nUtil.getMessages("app.fede.barchart.sales.axis"));
        xAxis.setTickAngle(SummaryHome.TICKANGLE);
        model.getAxes().put(AxisType.X, xAxis);
        Axis yAxis = model.getAxis(AxisType.Y);
        yAxis.setLabel(I18nUtil.getMessages("app.fede.barchart.sales.price.axis"));
        yAxis.setMin(Integer.valueOf(settingHome.getValue("app.fede.barchart.sales.min", "0")));
        double scale; //Aumentar un poco más la escala
        if (!product.getData().isEmpty()) {
            scale = (double) product.getData().values().toArray()[0] * Double.valueOf(settingHome.getValue("app.fede.barchart.sales.multiply.scale", "1.2"));
            yAxis.setMax(scale);
        }

        return model;
    }
    
    private ChartSeries createCategoriesSeries(String label, String queryNamed) {
        ChartSeries chartSerie = new ChartSeries();
        chartSerie.setLabel(label);

        int top = Integer.valueOf(settingHome.getValue("app.fede.inventory.top", "10"));
        List<Object[]> objects = productService.findObjectsByNamedQueryWithLimit(queryNamed, top, getStart(), getEnd());
//        System.out.println("\nObjects.size: "+objects.size());
        objects.stream().forEach((Object[] object) -> {
            chartSerie.set(object[0], ((BigDecimal) object[1]).doubleValue());
//            System.out.println("\nSerie: "+object[0]+"  "+((BigDecimal) object[1]).doubleValue());
        });
        return chartSerie;
    }

    private BarChartModel createbarModelCategory() {
        BarChartModel model = new BarChartModel();
        ChartSeries product = createCategoriesSeries(I18nUtil.getMessages("app.fede.barchart.sales.label.prices"), "Product.countProductByCategories");

        model.addSeries(product);
        model.setTitle(I18nUtil.getMessages("app.fede.barchart.sales.date.a") + Dates.toString(getStart(), settingHome.getValue("fede.name.pattern", "dd/MM/yyyy")) + " " + I18nUtil.getMessages("app.fede.barchart.sales.date.b") + Dates.toString(getEnd(), "dd/MM/yyyy"));
        model.setLegendPosition(settingHome.getValue("app.fede.barchart.sales.legendPosition", "e"));
        model.setStacked(false);
        model.setAnimate(true);
        model.setShowPointLabels(false);
        model.setExtender("skinBar2");

        Axis xAxis = new CategoryAxis(I18nUtil.getMessages("app.fede.barchart.sales.axis"));
        xAxis.setTickAngle(SummaryHome.TICKANGLE);
        model.getAxes().put(AxisType.X, xAxis);
        Axis yAxis = model.getAxis(AxisType.Y);
        yAxis.setLabel(I18nUtil.getMessages("app.fede.barchart.sales.price.axis"));
        yAxis.setMin(Integer.valueOf(settingHome.getValue("app.fede.barchart.sales.min", "0")));
        double scale; //Aumentar un poco más la escala
        if (!product.getData().isEmpty()) {
            scale = (double) product.getData().values().toArray()[0] * Double.valueOf(settingHome.getValue("app.fede.barchart.sales.multiply.scale", "1.2"));
            yAxis.setMax(scale);
        }

        return model;
    }

    private HorizontalBarChartModel createHorizontalBarModel() {
        HorizontalBarChartModel model = new HorizontalBarChartModel();

        model.addSeries(createProductsSeries(I18nUtil.getMessages("app.fede.barchart.sales.label.amount"), "Product.findTopProductIdsBetween"));
        model.addSeries(createProductsSeries(I18nUtil.getMessages("app.fede.barchart.sales.label.prices"), "Product.findTopProductIdsBetweenPrice"));

        model.setTitle(I18nUtil.getMessages("app.fede.barchart.sales.date.a") + Dates.toString(getStart(), settingHome.getValue("fede.name.pattern", "dd/MM/yyyy")) + " " + I18nUtil.getMessages("app.fede.barchart.sales.date.b") + Dates.toString(getEnd(), "dd/MM/yyyy"));
        model.setLegendPosition(settingHome.getValue("app.fede.barchart.sales.legendPosition", "e"));
        model.setStacked(false);
        model.setAnimate(true);
        model.setShadow(false);
        model.setShowPointLabels(true);
        model.setExtender("skinHorizontalBar");

        Axis xAxis = model.getAxis(AxisType.X);
        xAxis.setLabel(I18nUtil.getMessages("app.fede.barchart.sales.label.amount") + "/" + I18nUtil.getMessages("app.fede.barchart.sales.price.axis"));
        xAxis.setMin(0);
        double scale; //Aumentar un poco más la escala
        if (!model.getSeries().get(1).getData().isEmpty()) {
            scale = (double) model.getSeries().get(1).getData().values().toArray()[1] * Double.valueOf(settingHome.getValue("app.fede.barchart.sales.multiply.scale", "1.2"));
            xAxis.setMax(scale);
        }

        Axis yAxis = model.getAxis(AxisType.Y);

        yAxis.setLabel(I18nUtil.getMessages("app.fede.barchart.sales.axis"));
        yAxis.setMin(Integer.valueOf(settingHome.getValue("app.fede.barchart.sales.min", "0")));

        return model;
    }

    private ChartSeries createPurchasesSeries(String label, String queryNamed) {
        ChartSeries chartSerie = new ChartSeries();
        chartSerie.setLabel(label);

        int top = Integer.valueOf(settingHome.getValue("app.fede.inventory.top", "10"));
        List<Object[]> objects = facturaElectronicaService.findObjectsByNamedQueryWithLimit(queryNamed, top, getStart(), getEnd());
        objects.stream().forEach((Object[] object) -> {
            if (object[0] == null) {
                object[0] = object[2].toString() + " " + object[3].toString();
            }
            chartSerie.set(object[0], ((BigDecimal) object[1]).doubleValue());
        });
        setListPurchases(objects);
        return chartSerie;
    }

    private HorizontalBarChartModel createHorizontalPurchasesBarModel() {
        HorizontalBarChartModel model = new HorizontalBarChartModel();
        model.addSeries(createPurchasesSeries(I18nUtil.getMessages("ride.infoFactura.importeTotal"), "FacturaElectronica.findTopTotalBussinesEntityIdsBetween"));

        model.setTitle(I18nUtil.getMessages("app.fede.barchart.sales.date.a") + Dates.toString(getStart(), settingHome.getValue("fede.name.pattern", "dd/MM/yyyy")) + " " + I18nUtil.getMessages("app.fede.barchart.sales.date.b") + Dates.toString(getEnd(), "dd/MM/yyyy"));
        model.setLegendPosition(settingHome.getValue("app.fede.barchart.sales.legendPosition", "e"));
        model.setStacked(false);
        model.setAnimate(true);
        model.setShadow(false);
        model.setShowPointLabels(true);
        model.setExtender("skinHorizontalBar");

        Axis xAxis = model.getAxis(AxisType.X);
        xAxis.setLabel(I18nUtil.getMessages("ride.infoFactura.importeTotal"));
        xAxis.setMin(0);
        double scale; //Aumentar un poco más la escala
        if (!model.getSeries().get(0).getData().isEmpty()) {
            scale = (double) model.getSeries().get(0).getData().values().toArray()[0] * Double.valueOf(settingHome.getValue("app.fede.barchart.sales.multiply.scale", "1.2"));
            xAxis.setMax(scale);
        }
        Axis yAxis = model.getAxis(AxisType.Y);
        yAxis.setLabel(I18nUtil.getMessages("app.fede.barchart.sales.axis.provider"));
        yAxis.setMin(Integer.valueOf(settingHome.getValue("app.fede.barchart.sales.min", "0")));

        return model;
    }

    private LineChartModel createLineChartModel() {
        LineChartModel areaModel = new LineChartModel();

        boolean fillSeries = true;

        LineChartSeries fixedCosts = new LineChartSeries();
        fixedCosts.setFill(!fillSeries);
        fixedCosts.setLabel(I18nUtil.getMessages("app.fede.costs.fixed"));
        fixedCosts.setShowMarker(false);
        fixedCosts.setSmoothLine(false);

        LineChartSeries sales = new LineChartSeries();
        sales.setFill(fillSeries);
        sales.setLabel(I18nUtil.getMessages("app.fede.sales"));

        LineChartSeries purchases = new LineChartSeries();
        purchases.setFill(fillSeries);
        purchases.setLabel(I18nUtil.getMessages("common.purchases"));

        LineChartSeries profits = new LineChartSeries();
        profits.setFill(fillSeries);
        profits.setLabel(I18nUtil.getMessages("common.profit"));

        LineChartSeries customers = new LineChartSeries();
        customers.setFill(fillSeries);
        customers.setLabel(I18nUtil.getMessages("app.fede.sales.invoice.owners"));

        Date _start = getStart();
        if (Dates.calculateNumberOfDaysBetween(getStart(), getEnd()) <= 1) {
            int range = Integer.parseInt(settingHome.getValue("app.fede.chart.range", "7"));
            _start = Dates.addDays(getStart(), -1 * range);
        }
        Date _step = _start;
        String label = "";
        BigDecimal _salesTotal;
        Long _paxTotal;
        BigDecimal purchasesTotal;
        BigDecimal fixedCost = new BigDecimal(settingHome.getValue("app.fede.costs.fixed", "50"));
        for (int i = 0; i <= Dates.calculateNumberOfDaysBetween(_start, getEnd()); i++) {
            label = Strings.toString(_step, Calendar.DAY_OF_WEEK) + ", " + Dates.get(_step, Calendar.DAY_OF_MONTH);
            _salesTotal = calculeTotal(findInvoices(subject, DocumentType.INVOICE, Integer.MAX_VALUE, Dates.minimumDate(_step), Dates.maximumDate(_step)));
            _paxTotal = calculeTotalPax(findInvoices(subject, DocumentType.INVOICE, Integer.MAX_VALUE, Dates.minimumDate(_step), Dates.maximumDate(_step)));
            sales.set(label, _salesTotal);

            facturaElectronicaHome.setStart(Dates.minimumDate(_step));
            facturaElectronicaHome.setEnd(Dates.maximumDate(_step));
            purchasesTotal = facturaElectronicaHome.calculeTotal(facturaElectronicaHome.getResultList());

            fixedCosts.set(label, fixedCost);
            purchases.set(label, purchasesTotal);
            profits.set(label, _salesTotal.subtract(purchasesTotal)); //Utilidad bruta
            customers.set(label, _paxTotal); //Clientes

            _step = Dates.addDays(_step, 1); //Siguiente día
        }

        areaModel.addSeries(sales);
        areaModel.addSeries(profits);
        areaModel.addSeries(customers);
        areaModel.addSeries(fixedCosts);
        areaModel.addSeries(purchases);

        areaModel.setTitle(I18nUtil.getMessages("app.fede.chart.salesvspurchases"));
        areaModel.setLegendPosition(settingHome.getValue("app.fede.chart.legendPosition", "ne"));
        areaModel.setStacked(false);
        areaModel.setAnimate(false);
        areaModel.setZoom(true);
        areaModel.setExtender("skinChart");
        areaModel.setAnimate(false);
        areaModel.setShowPointLabels(false);

        Axis xAxis = new CategoryAxis(I18nUtil.getMessages("app.fede.chart.date.day.scale"));
        xAxis.setTickAngle(SummaryHome.TICKANGLE);
        areaModel.getAxes().put(AxisType.X, xAxis);
        Axis yAxis = areaModel.getAxis(AxisType.Y);
        yAxis.setLabel(I18nUtil.getMessages("app.fede.chart.sales.scale"));
        yAxis.setMin(Integer.valueOf(settingHome.getValue("app.fede.chart.sales.scale.min", "-250")));
        yAxis.setMax(Integer.valueOf(settingHome.getValue("app.fede.chart.sales.scale.max", "500")));

        return areaModel;
    }

    @Override
    protected void initializeDateInterval() {
        int range = 0; //Rango de fechas para visualiar lista de entidades
        try {
            range = Integer.valueOf(settingHome.getValue("fede.dashboard.summary.range", "7"));
        } catch (java.lang.NumberFormatException nfe) {
            nfe.printStackTrace();
            range = 1;
        }
        Calendar dayDate = Calendar.getInstance();
        setEnd(Dates.maximumDate(Dates.now()));
        setStart(Dates.minimumDate(Dates.addDays(getEnd(), -1 * (dayDate.get(Calendar.DAY_OF_MONTH) - 1))));
    }

    public List<Invoice> findInvoices(Subject author, DocumentType documentType, int limit, Date start, Date end) {
        if (author == null) { //retornar todas
            return invoiceService.findByNamedQueryWithLimit("Invoice.findByDocumentType", limit, documentType, true, start, end);
        } else {
            return invoiceService.findByNamedQueryWithLimit("Invoice.findByDocumentTypeAndAuthor", limit, documentType, author, true, start, end);
        }
    }

    public BigDecimal calculeTotal(List<Invoice> list) {

        BigDecimal subtotal = new BigDecimal(0);
        BigDecimal discount = new BigDecimal(0);
        Payment payment = null;
        for (Invoice i : list) {
            subtotal = subtotal.add(i.getTotal());
            payment = i.getPayments().isEmpty() ? null : i.getPayments().get(0);
            if (payment != null) {
                discount = discount.add(payment.getDiscount());
            }
        }

        return subtotal.subtract(discount, MathContext.UNLIMITED);
    }

    public Long calculeTotalPax(List<Invoice> list) {

        Long paxDay = 0L;
        for (Invoice i : list) {
            if (i.getPax() != null) {
                paxDay = paxDay + i.getPax();
            }
        }

        return paxDay;
    }
}
