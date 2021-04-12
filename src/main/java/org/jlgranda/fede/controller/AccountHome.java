/*
 * Copyright (C) 2021 jlgranda
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
package org.jlgranda.fede.controller;

import com.jlgranda.fede.SettingNames;
import com.jlgranda.fede.ejb.AccountService;
import com.jlgranda.fede.ejb.GeneralJournalService;
import com.jlgranda.fede.ejb.GroupService;
import com.jlgranda.fede.ejb.RecordDetailService;
import com.jlgranda.fede.ejb.RecordService;
import com.jlgranda.fede.ejb.sales.InvoiceService;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.model.DataModel;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import static javax.swing.UIManager.get;
import org.jlgranda.fede.controller.sales.SummaryHome;
import org.jlgranda.fede.model.accounting.Account;
import org.jlgranda.fede.model.accounting.GeneralJournal;
import org.jlgranda.fede.model.accounting.Record;
import org.jlgranda.fede.model.accounting.RecordDetail;
import org.jlgranda.fede.model.document.DocumentType;
import org.jlgranda.fede.model.document.EmissionType;
import org.jlgranda.fede.ui.model.LazyAccountDataModel;
import org.jpapi.util.I18nUtil;
import org.jpapi.model.BussinesEntity;
import org.jpapi.model.Group;
import org.jpapi.model.StatusType;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.Dates;
import org.primefaces.context.RequestContext;
import org.primefaces.event.NodeSelectEvent;
import org.primefaces.event.NodeUnselectEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.SortMeta;
import org.primefaces.model.SortOrder;
import org.primefaces.model.TreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jlgranda
 */
@Named
@ViewScoped
public class AccountHome extends FedeController implements Serializable {

    private static final long serialVersionUID = -1007161141552849702L;

    Logger logger = LoggerFactory.getLogger(AccountHome.class);

    @Inject
    private Subject subject;

    @Inject
    private SettingHome settingHome;

    @Inject
    private OrganizationData organizationData;

    @EJB
    private GroupService groupService;

    @EJB
    private InvoiceService invoiceService;

    @EJB
    private AccountService accountService;

    @EJB
    private GeneralJournalService journalService;

    @EJB
    private RecordService recordService;

    @EJB
    private RecordDetailService recordDetailService;

    private LazyAccountDataModel lazyDataModel;

    private TreeNode treeDataModel;
    private TreeNode singleSelectedNode;

    private Account account;
    private Account accountSelected;
    private Account parentAccount;
    private Account depositAccount;

    private Long accountId;

    //Calcular Resumen
    private BigDecimal grossSalesTotal;
    private BigDecimal discountTotal;
    private BigDecimal salesTotal;
    private BigDecimal purchaseTotal;
    private BigDecimal costTotal;
    private BigDecimal profilTotal;
    private Long paxTotal;
    private List<Object[]> listDiscount;

    //Calcular monto anterior de la cuenta por los recordDetails
    private BigDecimal amountDebeRdOld;
    private BigDecimal amountHaberRdOld;
    private BigDecimal amountDebeRd;
    private BigDecimal amountHaberRd;
    private List<Object[]> objectsRecordDetail;
    private BigDecimal amountAccount;

    @PostConstruct
    private void init() {
        int range = 0;
        try {
            range = Integer.valueOf(settingHome.getValue(SettingNames.ACCOUNT_TOP_RANGE, "7"));
        } catch (java.lang.NumberFormatException nfe) {
            nfe.printStackTrace();
            range = 7;
        }
        setEnd(Dates.maximumDate(Dates.now()));
        setStart(Dates.minimumDate(Dates.addDays(getEnd(), -1 * range)));
        setAccount(accountService.createInstance());//Instancia de Cuenta
        setOutcome("accounts");
        filter();

        setGrossSalesTotal(BigDecimal.ZERO);
        setDiscountTotal(BigDecimal.ZERO);
        setSalesTotal(BigDecimal.ZERO);
        setPurchaseTotal(BigDecimal.ZERO);
        setCostTotal(BigDecimal.ZERO);
        setProfilTotal(BigDecimal.ZERO);
        setPaxTotal(0L);
        calculeSummaryToday();
        this.treeDataModel = createTreeAccounts();
    }

    @Override
    public void handleReturn(SelectEvent event) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Group getDefaultGroup() {
        return this.defaultGroup;
    }

    @Override
    public List<Group> getGroups() {
        if (this.groups.isEmpty()) {
            //Todos los grupos para el modulo actual
            setGroups(groupService.findByOwnerAndModuleAndType(subject, settingHome.getValue(SettingNames.MODULE + "inventory", "inventory"), Group.Type.LABEL));
        }
        return this.groups;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Account getAccount() {
        if (this.accountId != null && !this.account.isPersistent()) {
            this.account = accountService.find(accountId);
        }
        this.parentAccount = accountService.findUniqueByNamedQuery("Account.findByIdAndOrg", this.account.getCuentaPadreId(), this.organizationData.getOrganization());
        if (this.parentAccount != null) {
            this.account.setCuentaPadreId(this.parentAccount.getId());
        }
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public List<Account> getAccounts() {
        return accountService.findByOrganization(this.organizationData.getOrganization());
    }

    public Account getParentAccount() {
        return this.parentAccount;
    }

    public void setParentAccount(Account parentAccount) {
        this.parentAccount = parentAccount;
    }

    public Account getAccountSelected() {
        return this.accountSelected;
    }

    public void setAccountSelected(Account accountSelected) {
        this.accountSelected = accountSelected;
    }

    public LazyAccountDataModel getLazyDataModel() {
        return lazyDataModel;
    }

    public void setLazyDataModel(LazyAccountDataModel lazyDataModel) {
        this.lazyDataModel = lazyDataModel;
    }

    public TreeNode getTreeDataModel() {
        return treeDataModel;
    }

    public void setTreeDataModel(TreeNode treeDataModel) {
        this.treeDataModel = treeDataModel;
    }

    public TreeNode getSingleSelectedNode() {
        return singleSelectedNode;
    }

    public void setSingleSelectedNode(TreeNode singleSelectedNode) {
        this.singleSelectedNode = singleSelectedNode;
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

    public List<Object[]> getObjectsRecordDetail() {
        return objectsRecordDetail;
    }

    public void setObjectsRecordDetail(List<Object[]> objectsRecordDetail) {
        this.objectsRecordDetail = objectsRecordDetail;
    }

    public BigDecimal getAmountDebeRdOld() {
        return amountDebeRdOld;
    }

    public void setAmountDebeRdOld(BigDecimal amountDebeRdOld) {
        this.amountDebeRdOld = amountDebeRdOld;
    }

    public BigDecimal getAmountHaberRdOld() {
        return amountHaberRdOld;
    }

    public void setAmountHaberRdOld(BigDecimal amountHaberRdOld) {
        this.amountHaberRdOld = amountHaberRdOld;
    }

    public BigDecimal getAmountAccount() {
        return amountAccount;
    }

    public void setAmountAccount(BigDecimal amountAccount) {
        this.amountAccount = amountAccount;
    }

    public Account getDepositAccount() {
        return depositAccount;
    }

    public void setDepositAccount(Account depositAccount) {
        this.depositAccount = depositAccount;
    }

    public void clear() {
        filter();
    }

    public void filter() {
        if (lazyDataModel == null) {
            lazyDataModel = new LazyAccountDataModel(accountService);
        }
//        lazyDataModel.setOwner(this.subject);
        lazyDataModel.setOrganization(this.organizationData.getOrganization());
        lazyDataModel.setStart(getStart());
        lazyDataModel.setEnd(getEnd());
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

    public TreeNode createTreeAccounts() {
        List<Account> accountsOrder = getAccounts();
        // Ordenar la lista por el atributo getCode(), para crear el árbol de cuentas correcto
        Collections.sort(accountsOrder, (Account account1, Account other) -> account1.getCode().compareToIgnoreCase(other.getCode()));
        TreeNode generalTree = new DefaultTreeNode(new Account("Código", "Cuenta"), null); //Árbol general
        TreeNode parent = null;
        TreeNode child = null;
        int iterador = 0;
        while (iterador < accountsOrder.size()) {
            if (accountsOrder.get(iterador).getCuentaPadreId() == null) {
                parent = new DefaultTreeNode(accountsOrder.get(iterador), generalTree);
                iterador++;
            } else {
                while (accountsOrder.get(iterador).getCuentaPadreId() != null) {
                    child = new DefaultTreeNode(accountsOrder.get(iterador), parent);
                    iterador++;
                }
            }
        }
        return generalTree;
    }
    
    public boolean mostrarFormulario(Map<String, List<String>> params) {
        String width = settingHome.getValue(SettingNames.POPUP_WIDTH, "800");
        String height = settingHome.getValue(SettingNames.POPUP_HEIGHT, "600");
        String left = settingHome.getValue(SettingNames.POPUP_LEFT, "0");
        String top = settingHome.getValue(SettingNames.POPUP_TOP, "0");
        super.openDialog(SettingNames.POPUP_FORMULARIO_GENERALLEDGER, width, height, left, top, true, params);
        return true;
    }

    public boolean mostrarFormularioLedger(Account account) {
        if(account!=null){
            this.accountSelected = account;
        }
        if (this.accountSelected != null) {
            super.setSessionParameter("account", this.accountSelected);
        }

        return mostrarFormulario(null);
    }
    
     public void closeFormularioRecord(Object data) {
        removeSessionParameter("account");
        super.closeDialog(data);
    }

    public void loadSessionParameters() {

        if (existsSessionParameter("account")) {
            this.setAccountSelected((Account) getSessionParameter("account"));
            this.getAccountSelected(); //Carga el objeto persistente
        }
    }

    public void findRecordDetailAccountTop() {
        this.amountDebeRdOld = BigDecimal.ZERO;
        this.amountHaberRdOld = BigDecimal.ZERO;
        Calendar dayDate = Calendar.getInstance();
        Date _end = Dates.maximumDate(Dates.now());
        Date _start = Dates.minimumDate(Dates.addDays(getEnd(), -1 * (dayDate.get(Calendar.DAY_OF_MONTH) - 1)));
        Date _end1 = Dates.maximumDate(Dates.addDays(_start, -1));
        //Obtener los recordDetails desde el mes anterior hasta el inicial
        List<RecordDetail> recordDetailsOld = recordDetailService.findByNamedQuery("RecordDetail.findByTopAccountAndOrg", this.accountSelected, _end1, this.organizationData.getOrganization());
        for (int i = 0; i < recordDetailsOld.size(); i++) {
            if (recordDetailsOld.get(i).getRecordDetailType() == RecordDetail.RecordTDetailType.DEBE) {
                this.amountDebeRdOld = this.amountDebeRdOld.add(recordDetailsOld.get(i).getAmount());
            } else if (recordDetailsOld.get(i).getRecordDetailType() == RecordDetail.RecordTDetailType.HABER) {
                this.amountHaberRdOld = this.amountHaberRdOld.add(recordDetailsOld.get(i).getAmount());
            }
        }
        //Obtener los recordsDetails del mes actual
        this.objectsRecordDetail = recordDetailService.findByNamedQuery("RecordDetail.findByTopAccAndOrg", this.accountSelected, _start, _end, this.organizationData.getOrganization());
        this.amountAccount = BigDecimal.ZERO;
        this.amountDebeRd = BigDecimal.ZERO;
        this.amountHaberRd = BigDecimal.ZERO;
        this.objectsRecordDetail.stream().forEach((Object[] object) -> {
            if (object[3] == RecordDetail.RecordTDetailType.DEBE) {
                this.amountDebeRd = this.amountDebeRd.add((BigDecimal) object[2]);
            } else if (object[3] == RecordDetail.RecordTDetailType.HABER) {
                this.amountHaberRd = this.amountHaberRd.add((BigDecimal) object[2]);
            }
        });
        this.amountAccount = (this.amountDebeRdOld.add(this.amountDebeRd)).subtract(this.amountHaberRdOld.add(this.amountHaberRd));
    }

    public void validateDeposit() {
        if (this.depositAccount != null) {
            if (this.amountAccount.compareTo(BigDecimal.ZERO) == 0) {
                this.addWarningMessage(I18nUtil.getMessages("action.warning"), I18nUtil.getMessages("app.fede.accouting.account.amount"));
            } else {
                if (this.accountSelected.getId().compareTo(this.depositAccount.getId())==0) {
                    this.addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("app.fede.accouting.account.isEquals"));
                } else {
                    registerRecordInJournal();
                    findRecordDetailAccountTop();
                }
            }
        } else {
            this.addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("app.fede.accouting.account.validate"));
        }
    }

    public void registerRecordInJournal() {
//        Crear o encontrar el journal y el record, para insertar los recordDetails
        GeneralJournal journal = buildFindJournal();
        Record record = buildRecord();
//        Crear/Modificar y anadir un recordDetail al record
        record.addRecordDetail(updateRecordDetail(this.accountSelected.getName()));
        record.addRecordDetail(updateRecordDetail(this.depositAccount.getName()));
        record.setDescription("Transferencia del valor de " + this.accountSelected.getName() + " hacia " + this.depositAccount.getName());
        journal.addRecord(record); //Agregar el record al journal

        journalService.save(journal.getId(), journal); //Guardar el journal

    }

    private GeneralJournal buildFindJournal() {
        GeneralJournal generalJournal = journalService.findUniqueByNamedQuery("Journal.findByCreatedOnAndOrg", Dates.minimumDate(Dates.now()), Dates.now(), this.organizationData.getOrganization());
        if (generalJournal == null) {
            generalJournal = journalService.createInstance();
            generalJournal.setOrganization(this.organizationData.getOrganization());
            generalJournal.setOwner(subject);
            generalJournal.setCode(UUID.randomUUID().toString());
            generalJournal.setName(I18nUtil.getMessages("app.fede.accouting.journal") + " " + this.organizationData.getOrganization().getInitials() + "/" + Dates.toDateString(Dates.now()));
            journalService.save(generalJournal); //Guardar el journal creado
            generalJournal = journalService.findUniqueByNamedQuery("Journal.findByCreatedOnAndOrg", Dates.minimumDate(Dates.now()), Dates.now(), this.organizationData.getOrganization());
        }
        return generalJournal;
    }

    private Record buildRecord() {
        Record recordGeneral = recordService.createInstance();
        recordGeneral.setOwner(subject);
        return recordGeneral;
    }

    private RecordDetail updateRecordDetail(String NameAccount) {
        RecordDetail recordDetailGeneral = recordDetailService.createInstance();
        if (this.amountAccount.compareTo(BigDecimal.ZERO) == 1) {
            recordDetailGeneral.setOwner(subject);
            recordDetailGeneral.setAmount(this.amountAccount.abs());
            if (NameAccount.equals(this.accountSelected.getName())) {
                recordDetailGeneral.setAccount(this.accountSelected);
                recordDetailGeneral.setRecordDetailType(RecordDetail.RecordTDetailType.HABER);
            } else if (NameAccount.equals(this.depositAccount.getName())) {
                recordDetailGeneral.setAccount(this.depositAccount);
                recordDetailGeneral.setRecordDetailType(RecordDetail.RecordTDetailType.DEBE);
            }
        }
        return recordDetailGeneral;
    }

    public void onNodeSelect(NodeSelectEvent event) {
        try {
            if (event != null && event.getTreeNode().getData() != null) {
                Account p = (Account) event.getTreeNode().getData();
                redirectTo("/pages/fede/accounting/account.jsf?accountId=" + p.getId());
            }
        } catch (IOException ex) {
            logger.error("No fue posible seleccionar las {} con nombre {}" + I18nUtil.getMessages("BussinesEntity"), ((BussinesEntity) event.getTreeNode()).getName());
        }
    }

//    public void onRowSelect(SelectEvent event) {
//        try {
//            //Redireccionar a RIDE de objeto seleccionado
//            if (event != null && event.getObject() != null) {
//                Account p = (Account) event.getObject();
//                redirectTo("/pages/fede/accounting/account.jsf?accountId=" + p.getId());
//            }
//        } catch (IOException ex) {
//            logger.error("No fue posible seleccionar las {} con nombre {}" + I18nUtil.getMessages("BussinesEntity"), ((BussinesEntity) event.getObject()).getName());
//        }
//    }
    public void save() {
        if (account.isPersistent()) {
            account.setLastUpdate(Dates.now());
        } else {
            account.setAuthor(this.subject);
            account.setOwner(this.subject);
            account.setOrganization(this.organizationData.getOrganization());
        }
        accountService.save(account.getId(), account);
    }

    public List<Object[]> getListDiscount() {
        return listDiscount;
    }

    public List<Object[]> getListDiscount(Date _start, Date _end) {
//        List<Object[]> objects = invoiceService.findObjectsByNamedQueryWithLimit("Invoice.findTotalInvoiceBussinesSalesDiscountBetween", Integer.MAX_VALUE, this.subject, DocumentType.INVOICE, StatusType.CLOSE.toString(), _start, _end, BigDecimal.ZERO);
        List<Object[]> objects = invoiceService.findObjectsByNamedQueryWithLimit("Invoice.findTotalInvoiceBussinesSalesDiscountBetweenOrg", Integer.MAX_VALUE, this.organizationData.getOrganization(), DocumentType.INVOICE, StatusType.CLOSE.toString(), _start, _end, BigDecimal.ZERO);
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

    public void calculeSummaryToday() {
        Date _start = Dates.minimumDate(getEnd());
        Date _end = Dates.maximumDate(getEnd());
        calculeSummary(_start, _end);
        setListDiscount(getListDiscount(_start, _end));
    }

    public void calculeSummary(Date _start, Date _end) {

        this.costTotal = BigDecimal.ZERO;
//        List<Object[]> objects = invoiceService.findObjectsByNamedQueryWithLimit("Invoice.findTotalInvoiceSalesDiscountBetween", Integer.MAX_VALUE, this.subject, DocumentType.INVOICE, StatusType.CLOSE.toString(), _start, _end);
        List<Object[]> objects = invoiceService.findObjectsByNamedQueryWithLimit("Invoice.findTotalInvoiceSalesDiscountBetweenOrg", Integer.MAX_VALUE, this.organizationData.getOrganization(), DocumentType.INVOICE, StatusType.CLOSE.toString(), _start, _end);
        objects.stream().forEach((Object[] object) -> {
            this.grossSalesTotal = (BigDecimal) object[0];
            this.discountTotal = (BigDecimal) object[1];
            this.salesTotal = (BigDecimal) object[2];
        });
//        objects = invoiceService.findObjectsByNamedQueryWithLimit("FacturaElectronica.findTotalByEmissionTypeBetween", Integer.MAX_VALUE, this.subject, _start, _end, EmissionType.PURCHASE_CASH);
        objects = invoiceService.findObjectsByNamedQueryWithLimit("FacturaElectronica.findTotalByEmissionTypeBetweenOrg", Integer.MAX_VALUE, this.organizationData.getOrganization(), _start, _end, EmissionType.PURCHASE_CASH);
        objects.stream().forEach((Object object) -> {
            this.purchaseTotal = (BigDecimal) object;
        });
//        objects = invoiceService.findObjectsByNamedQueryWithLimit("Invoice.findTotalInvoiceSalesPaxBetween", Integer.MAX_VALUE, this.subject, DocumentType.INVOICE, StatusType.CLOSE.toString(), _start, _end);
        objects = invoiceService.findObjectsByNamedQueryWithLimit("Invoice.findTotalInvoiceSalesPaxBetweenOrg", Integer.MAX_VALUE, this.organizationData.getOrganization(), DocumentType.INVOICE, StatusType.CLOSE.toString(), _start, _end);
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

    @Override
    protected void initializeDateInterval() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
