/*
 * Copyright (C) 2016 jlgranda
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.jlgranda.fede.controller.inventory;

import com.jlgranda.fede.SettingNames;
import com.jlgranda.fede.ejb.GroupService;
import com.jlgranda.fede.ejb.sales.ProductCache;
import com.jlgranda.fede.ejb.sales.ProductService;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.faces.model.SelectItem;
import javax.inject.Inject;
import javax.inject.Named;
import org.jlgranda.fede.controller.FedeController;
import org.jlgranda.fede.controller.SettingHome;
import org.jlgranda.fede.model.sales.Product;
import org.jlgranda.fede.model.sales.ProductType;
import org.jlgranda.fede.ui.model.LazyProductDataModel;
import org.jlgranda.fede.ui.util.UI;
import org.jpapi.model.BussinesEntity;
import org.jpapi.model.Group;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.Dates;
import org.jpapi.util.I18nUtil;
import org.jpapi.util.Lists;
import org.jpapi.util.QueryData;
import org.jpapi.util.QuerySortOrder;
import org.jpapi.util.Strings;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.chart.Axis;
import org.primefaces.model.chart.AxisType;
import org.primefaces.model.chart.BarChartModel;
import org.primefaces.model.chart.BarChartSeries;
import org.primefaces.model.chart.CategoryAxis;
import org.primefaces.model.chart.ChartSeries;
import org.primefaces.model.chart.LineChartModel;
import org.primefaces.model.chart.LineChartSeries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jlgranda
 */
@Named
@ViewScoped
public class InventoryHome extends FedeController implements Serializable {

    private static final long serialVersionUID = -21640696368253046L;

    Logger logger = LoggerFactory.getLogger(InventoryHome.class);

    @Inject
    private SettingHome settingHome;

    @EJB
    private GroupService groupService;

    private Long productId;

    private Product lastProduct;

    private Product product;

    private ProductType productType;

    private List<Product> lastProducts = new ArrayList<>();

    private LazyProductDataModel lazyDataModel;

    @EJB
    private ProductService productService;

    @EJB
    private ProductCache productCache;

    @Inject
    private Subject subject;

    private String mode;

    private Group groupSelected;

    @PostConstruct
    private void init() {
        int range = 0; //Rango de fechas para visualiar lista de entidades
        try {
            range = Integer.valueOf(settingHome.getValue(SettingNames.PRODUCT_TOP_RANGE, "7"));
        } catch (java.lang.NumberFormatException nfe) {
            nfe.printStackTrace();
            range = 7;
        }
        setEnd(Dates.maximumDate(Dates.now()));
//        setStart(Dates.minimumDate(Dates.addDays(getEnd(), -1 * range)));
//        setStart(Dates.minimumDate(Dates.addDays(getLastProduct().getCreatedOn(),0)));

        setProduct(productService.createInstance());
        getProduct().setProductType(ProductType.PRODUCT);
        setProductType(ProductType.PRODUCT);

        //Lista de productos a gráficar por defecto
        List<BussinesEntity> defaultProducts = new ArrayList<>();
        defaultProducts.add(productCache.lookup(80L)); //Queso
        defaultProducts.add(productCache.lookup(81L)); //Cebolla
        defaultProducts.add(productCache.lookup(370L)); //Empapizza
        //defaultProducts.add(productService.find(8005L)); //Empapizza de pollo
        //defaultProducts.add(productService.find(47763L)); //Empapizza de tocino
        //defaultProducts.add(productService.find(4563L)); //Verde
        //defaultProducts.add(productService.find(6660L)); //Tamal
        //defaultProducts.add(productService.find(6846L)); //Humita
        defaultProducts.add(productCache.lookup(87L)); //Chocolate
        defaultProducts.add(productCache.lookup(101L)); //Cafe
        defaultProducts.add(productCache.lookup(78L)); //Capuchino
        //defaultProducts.add(productService.find(416L)); //Jugos
        //defaultProducts.add(productService.find(39640L)); //Frapuchino
        //defaultProducts.add(productService.find(39527L)); //Helado

        setSelectedBussinesEntities(defaultProducts);

        setMode("app.fede.chart.gap.total");

        setOutcome("inventory-inbox");
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public ProductType getProductType() {
        return productType;
    }

    public void setProductType(ProductType productType) {
        this.productType = productType;
    }

    public Product getLastProduct() {
        if (lastProduct == null) {
            List<Product> obs = productService.findByNamedQuery("Product.findLastProduct", 1);
            lastProduct = obs.isEmpty() ? new Product() : (Product) obs.get(0);
        }
        return lastProduct;
    }

    public void setLastProduct(Product lastProduct) {
        this.lastProduct = lastProduct;
    }

    public Product getProduct() {
        if (this.productId != null && !this.product.isPersistent()) {
            this.product = productService.find(productId);
//            product.add(groupSelected);
            if (!productService.find(productId).getGroups().isEmpty()) {
                setGroupSelected(this.product.getGroups().get(0));
            }
        }
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public void setLastProducts(List<Product> lastProducts) {
        this.lastProducts = lastProducts;
    }

    public List<Product> getLastProducts() {
        int limit = Integer.parseInt(settingHome.getValue("app.fede.sales.dashboard.lasts.list.length", "10"));
        if (lastProducts.isEmpty()) {
            lastProducts = productService.findByNamedQuery("Product.findLastProducts", limit);
        }
        return lastProducts;
    }

    public String getSelectedKeys() {
        String _keys = "";
        if (getSelectedBussinesEntities() != null && !getSelectedBussinesEntities().isEmpty()) {
            _keys = Lists.toString(getSelectedBussinesEntities());
        }
        return _keys;
    }

    public Group getGroupSelected() {
        return groupSelected;
    }

    public void setGroupSelected(Group groupSelected) {
        this.groupSelected = groupSelected;
    }

    public boolean mostrarFormularioProducto() {
        String width = settingHome.getValue(SettingNames.POPUP_WIDTH, "550");
        String height = settingHome.getValue(SettingNames.POPUP_HEIGHT, "480");
        super.openDialog(settingHome.getValue("app.fede.inventory.popup", "popup_producto"), width, height, true);
        return true;
    }

    @Override
    public void handleReturn(SelectEvent event) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void save() {

        if (product.isPersistent()) {
            product.setLastUpdate(Dates.now());
        } else {
            product.setAuthor(this.subject);
            product.setOwner(this.subject);
        }
//        productService.save(product.getId(), product);
        product.add(groupSelected); //Añadir el ggroup (tipo) seleccionado al producto
        productService.save(product.getId(), product); //Volver a guardar el producto para almacenar el ggroup
    }

    @Override
    public Group getDefaultGroup() {
        return this.defaultGroup;
    }

    /**
     * Retorna los grupos para este controlador
     *
     * @return
     */
    @Override
    public List<Group> getGroups() {
        if (this.groups.isEmpty()) {
            //Todos los grupos para el modulo actual
            setGroups(groupService.findByOwnerAndModuleAndType(subject, settingHome.getValue(SettingNames.MODULE + "inventory", "inventory"), Group.Type.LABEL));
        }

        return this.groups;
    }

    public LazyProductDataModel getLazyDataModel() {
        filter();
        return lazyDataModel;
    }

    public void setLazyDataModel(LazyProductDataModel lazyDataModel) {
        this.lazyDataModel = lazyDataModel;
    }

    /**
     * Busca objetos <tt>Product</tt> para la clave de búsqueda en las columnas
     * name y code
     *
     * @param keyword
     * @return una lista de objetos <tt>Product</tt> que coinciden con la
     * palabra clave dada.
     */
    public List<Product> find(String keyword) {

        return productCache.lookup(keyword, ProductType.PRODUCT); //sólo productos
//        keyword = keyword.trim();
//        Map<String, Object> filters = new HashMap<>();
//        Map<String, String> columns = new HashMap<>();
//        columns.put("code", keyword);
//        columns.put("name", keyword);
//        filters.put("productType", getProductType());
//        filters.put("dummy", columns);
//        QueryData<Product> queryData = productService.find(-1, -1, "code, name", QuerySortOrder.ASC, filters);
//        return queryData.getResult();
    }

    /**
     * TODO obtener el top 10 de productos, esta implementación es ineficiente
     * recupera los objetos de uno en uno.
     *
     * @return
     */
    public List<Product> findTop() {
        int top = Integer.valueOf(settingHome.getValue("app.fede.inventory.top", "15"));
        List<Object[]> objects = productService.findObjectsByNamedQueryWithLimit("Product.findTopProductIdsBetween", top, getStart(), getEnd());
        List<Product> result = new ArrayList<>();
        objects.stream().forEach((Object[] object) -> {
            Product _product = productCache.lookup((Long) object[0]);
            if (_product != null) {
                _product.getStatistics().setCount((Double) object[1]);
                result.add(_product);
            }
        });
        return result;
    }

    public List<Product> findLastProductsByType(String type) {
        Map<String, Object> filters = new HashMap<>();
        Map<String, String> columns = new HashMap<>();
        filters.put("productType", ProductType.valueOf(type));
        filters.put("dummy", columns);
        QueryData<Product> queryData = productService.find(-1, -1, "name", QuerySortOrder.ASC, filters);
        return queryData.getResult();
    }

    private Double countProduct(Long id, Date minimumDate, Date maximumDate) {
        List<Object[]> objects = productService.findObjectsByNamedQueryWithLimit("Product.countProduct", 0, id, minimumDate, maximumDate);
        Double result = Double.valueOf(0);
        for (Object[] object : objects) {
            result = (Double) object[1];
        }

        return result;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public SelectItem[] getModesAsSelectItem() {
        return UI.getSettingAsSelectItems(settingHome.findSettings("app.fede.chart.gap."), true);
    }

    /**
     * Filtro que llena el Lazy Datamodel
     */
    private void filter() {
        if (lazyDataModel == null) {
            lazyDataModel = new LazyProductDataModel(productService);
        }
        lazyDataModel.setOwner(subject);
        lazyDataModel.setProductType(getProductType());
        lazyDataModel.setStart(this.getStart());
        lazyDataModel.setEnd(this.getEnd());

        if (getKeyword() != null && getKeyword().startsWith("label:")) {
            String parts[] = getKeyword().split(":");
            if (parts.length > 1) {
                lazyDataModel.setTags(parts[1]);
            }
            lazyDataModel.setFilterValue(null);//No buscar por keyword
        } else {
            lazyDataModel.setTags(getTags());
            lazyDataModel.setFilterValue(getKeyword());
        }

    }

    public void onRowSelect(SelectEvent event) {
        try {
            //Redireccionar a RIDE de objeto seleccionado
            if (event != null && event.getObject() != null) {
                Product p = (Product) event.getObject();
                redirectTo("/pages/fede/inventory/product.jsf?productId=" + p.getId());
            }
        } catch (IOException ex) {
            logger.error("No fue posible seleccionar las {} con nombre {}" + I18nUtil.getMessages("BussinesEntity"), ((BussinesEntity) event.getObject()).getName());
        }
    }

    public void cleanChartModels() {
        //dummy
    }

    /**
     * Limpiar para refrescar vista
     */
    public void clear() {
        filter();
    }

    ////////////////////////////////////////////////////////////////////////////
    //Charts
    ////////////////////////////////////////////////////////////////////////////
    private BarChartModel topBarChartModel;

    public BarChartModel getTopBarChartModel() {
        if (topBarChartModel == null) {
            setTopBarChartModel(createBarModel());
        }
        return topBarChartModel;
    }

    public void setTopBarChartModel(BarChartModel topBarChartModel) {
        this.topBarChartModel = topBarChartModel;
    }

    private BarChartModel createBarModel() {
        BarChartModel barModel = initBarModel();

        barModel.setTitle(settingHome.getValue("app.fede.chart.top.products", "Lo más vendido"));
        barModel.setLegendPosition(settingHome.getValue("app.fede.chart.legendPosition", "nw"));
        barModel.setExtender("skinBarChart");
        barModel.setAnimate(false);
        barModel.setShowPointLabels(false);

        Axis xAxis = barModel.getAxis(AxisType.X);
        xAxis.setLabel(settingHome.getValue("app.fede.chart.top.products.xaxis.label", "Productos"));

        Axis yAxis = barModel.getAxis(AxisType.Y);
        yAxis.setLabel(settingHome.getValue("app.fede.chart.top.products.yaxis.label", "Cantidad"));
        yAxis.setMin(0);
        //yAxis.setMax(settingHome.getValue("app.fede.chart.sales.scale.max", "200"));
        return barModel;
    }

    private BarChartModel initBarModel() {
        BarChartModel model = new BarChartModel();

        ChartSeries products = new ChartSeries();
        products.setLabel(settingHome.getValue("app.fede.chart.top.units", "Cantidad"));
        int top = Integer.valueOf(settingHome.getValue("app.fede.inventory.top", "10"));
        List<Object[]> objects = productService.findObjectsByNamedQueryWithLimit("Product.findTopProductNames", top, getStart(), getEnd());
        objects.stream().forEach((object) -> {
            products.set(object[0], (Number) object[1]);
        });
        model.addSeries(products);

        return model;
    }

    public LineChartModel buildLineBarChartModel(List<BussinesEntity> selectedBussinesEntities, String skinChart) {
        return createLineChartModel(selectedBussinesEntities, skinChart);
    }

    public BarChartModel buildBarChartModel(List<BussinesEntity> selectedBussinesEntities, String skinChart) {
        return createBarChartModel(selectedBussinesEntities, skinChart);
    }

    private LineChartModel createLineChartModel(List<BussinesEntity> selectedBussinesEntities, String skinChart) {
        LineChartModel areaModel = new LineChartModel();

        if (selectedBussinesEntities == null || selectedBussinesEntities.isEmpty()) {
            return areaModel;
        }

        LineChartSeries product = null;
        Date _start = getStart();
        Date _step = null;
        String label = "";
        Double total;
        if (Dates.calculateNumberOfDaysBetween(getStart(), getEnd()) <= 1) {
            int range = Integer.parseInt(settingHome.getValue("app.fede.chart.range", "7"));
            _start = Dates.addDays(getStart(), -1 * range);
        }

        for (BussinesEntity entity : selectedBussinesEntities) {
            product = new LineChartSeries();
            product.setFill(false);
            product.setLabel(entity.getName());
            _step = _start;
            for (int i = 0; i <= Dates.calculateNumberOfDaysBetween(_start, getEnd()); i++) {
                label = Strings.toString(_step, Calendar.DAY_OF_WEEK) + ", " + Dates.get(_step, Calendar.DAY_OF_MONTH);
                total = countProduct(entity.getId(), Dates.minimumDate(_step), Dates.maximumDate(_step));
                product.set(label, total);
                _step = Dates.addDays(_step, 1); //Siguiente día
            }

            areaModel.addSeries(product);

        }

        areaModel.setTitle(I18nUtil.getMessages("app.fede.chart.products.history"));
        areaModel.setLegendPosition(settingHome.getValue("app.fede.chart.legendPosition", "nw"));
        areaModel.setExtender(skinChart);
        areaModel.setAnimate(false);
        areaModel.setShowPointLabels(false);

        Axis xAxis = new CategoryAxis(I18nUtil.getMessages("app.fede.chart.date.day.scale"));
        areaModel.getAxes().put(AxisType.X, xAxis);
        Axis yAxis = areaModel.getAxis(AxisType.Y);
        yAxis.setLabel(I18nUtil.getMessages("app.fede.chart.sales.scale"));
        yAxis.setMin(0);

        return areaModel;
    }

    private BarChartModel createBarChartModel(List<BussinesEntity> selectedBussinesEntities, String skinChart, String mode) {
        BarChartModel areaModel = new BarChartModel();

        if (selectedBussinesEntities == null || selectedBussinesEntities.isEmpty()) {
            return areaModel;
        }

        BarChartSeries product = null;
        Date _start = getStart();
        Date _step = null;
        String label = "";
        Double total;
        int gap = Integer.parseInt(settingHome.getValue(getMode(), "7"));

        if (Dates.calculateNumberOfDaysBetween(getStart(), getEnd()) <= 1) {
            int range = Integer.parseInt(settingHome.getValue(getMode(), "7"));
            _start = Dates.addDays(getStart(), -1 * range);
        }

        for (BussinesEntity entity : selectedBussinesEntities) {
            product = new BarChartSeries();
            //product.setFill(false);
            product.setLabel(entity.getName());
            _step = _start;
            for (int i = 0; i <= Dates.calculateNumberOfDaysBetween(_start, getEnd()); i++) {
                label = Strings.toString(_step, Calendar.DAY_OF_WEEK) + ", " + Dates.get(_step, Calendar.DAY_OF_MONTH);
                total = countProduct(entity.getId(), Dates.minimumDate(_step), Dates.maximumDate(_step));
                product.set(label, total);
                _step = Dates.addDays(_step, gap); //Siguiente día
            }

            areaModel.addSeries(product);

        }

        areaModel.setTitle(I18nUtil.getMessages("app.fede.chart.products.history"));
        areaModel.setLegendPosition(settingHome.getValue("app.fede.chart.legendPosition", "nw"));
        areaModel.setExtender(skinChart);
        areaModel.setAnimate(false);
        areaModel.setShowPointLabels(false);

        Axis xAxis = new CategoryAxis(I18nUtil.getMessages("app.fede.chart.date.day.scale"));
        areaModel.getAxes().put(AxisType.X, xAxis);
        Axis yAxis = areaModel.getAxis(AxisType.Y);
        yAxis.setLabel(I18nUtil.getMessages("app.fede.chart.sales.scale"));
        yAxis.setMin(0);

        return areaModel;
    }

    private BarChartModel createBarChartModel(List<BussinesEntity> selectedBussinesEntities, String skinChart) {
        return createBarChartModel(selectedBussinesEntities, skinChart, "journal");
    }

    private BarChartModel createProductBarChartModel(String skinChart) {
        BarChartModel areaModel = new BarChartModel();

        if (selectedBussinesEntities == null || selectedBussinesEntities.isEmpty()) {
            return areaModel;
        }

        BarChartSeries product = null;
        Date _start = getStart();
        Date _step = null;
        String label = "";
        Double total;
        int gap = Integer.parseInt(settingHome.getValue(getMode(), "7"));

        if (Dates.calculateNumberOfDaysBetween(getStart(), getEnd()) <= 1) {
            int range = Integer.parseInt(settingHome.getValue(getMode(), "7"));
            _start = Dates.addDays(getStart(), -1 * range);
        }

        for (BussinesEntity entity : selectedBussinesEntities) {
            product = new BarChartSeries();
            //product.setFill(false);
            product.setLabel(entity.getName());
            _step = _start;
            for (int i = 0; i <= Dates.calculateNumberOfDaysBetween(_start, getEnd()); i++) {
                label = Strings.toString(_step, Calendar.DAY_OF_WEEK) + ", " + Dates.get(_step, Calendar.DAY_OF_MONTH);
                total = countProduct(entity.getId(), Dates.minimumDate(_step), Dates.maximumDate(_step));
                product.set(label, total);
                _step = Dates.addDays(_step, gap); //Siguiente día
            }

            areaModel.addSeries(product);

        }

        areaModel.setTitle(I18nUtil.getMessages("app.fede.chart.products.history"));
        areaModel.setLegendPosition(settingHome.getValue("app.fede.chart.legendPosition", "nw"));
        areaModel.setExtender(skinChart);
        areaModel.setAnimate(false);
        areaModel.setShowPointLabels(false);

        Axis xAxis = new CategoryAxis(I18nUtil.getMessages("app.fede.chart.date.day.scale"));
        areaModel.getAxes().put(AxisType.X, xAxis);
        Axis yAxis = areaModel.getAxis(AxisType.Y);
        yAxis.setLabel(I18nUtil.getMessages("app.fede.chart.sales.scale"));
        yAxis.setMin(0);

        return areaModel;
    }

    public BarChartModel buildProductBarChartModel() {
        setStart(Dates.minimumDate(getStart()));
        setEnd(Dates.maximumDate(getEnd()));
        return buildBarChartModel(getSelectedBussinesEntities(), "skinBarChart");
    }

    @Override
    protected void initializeDateInterval() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
