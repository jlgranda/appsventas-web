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
import com.jlgranda.fede.ejb.accounting.AccountCache;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.jlgranda.fede.model.accounting.Account;
import org.jlgranda.fede.model.accounting.GeneralJournal;
import org.jlgranda.fede.model.accounting.Record;
import org.jlgranda.fede.model.accounting.RecordDetail;
import org.jlgranda.fede.ui.model.LazyAccountDataModel;
import org.jpapi.util.I18nUtil;
import org.jpapi.model.BussinesEntity;
import org.jpapi.model.Group;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.Dates;
import org.primefaces.event.NodeSelectEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.DefaultTreeNode;
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

    @EJB
    AccountCache accountCache;

    @Inject
    private Subject subject;

    @Inject
    private SettingHome settingHome;

    @Inject
    private OrganizationData organizationData;

    @EJB
    private GroupService groupService;

    @EJB
    private AccountService accountService;

    @EJB
    private GeneralJournalService journalService;

    @EJB
    private RecordService recordService;

    @EJB
    private RecordDetailService recordDetailService;

    //Modelo de datos lazy
    private LazyAccountDataModel lazyDataModel;

    //Modelo de árbol de datos
    private TreeNode treeDataModel;
    private TreeNode singleSelectedNode;

    //Objetos Account para edición
    private Long accountId;
    private Long parentAccountId;
    private Account account;
    private Account accountSelected;
    private Account parentAccount;
    private Account depositAccount;

    //Calcular el GeneralLedger: Montos (recordDetails) de Accounts 
    private BigDecimal amountDebeRdOld;
    private BigDecimal amountHaberRdOld;
    private BigDecimal amountDebeRd;
    private BigDecimal amountHaberRd;
    private List<Object[]> objectsRecordDetail;
    private BigDecimal amountAccount;

    //Reporte de accounts con recordDetails
    private List<RecordDetail> recordDetailsAccount;
    /**
     * El objeto Record para edición
     */
    private Record record;

    /**
     * RecordDetail para edición
     */
    private RecordDetail recordDetail;

    private Long recordId;

    /**
     * Valor de la cuenta principal
     */
    private BigDecimal fundAccountMain;

    /*  
     * Lista con los recordsDetails true
     */
    private List<RecordDetail> recordDetailsUpdate;

    private int rangeReport;

    @PostConstruct
    private void init() {

        setAccount(accountService.createInstance());
        setParentAccount(accountService.createInstance());

        int range = 0;
        try {
            range = Integer.valueOf(settingHome.getValue(SettingNames.ACCOUNT_TOP_RANGE, "7"));
        } catch (java.lang.NumberFormatException nfe) {
            nfe.printStackTrace();
            range = 7;
        }
        //Inicialización de variables, objetos, métodos.
        setEnd(Dates.maximumDate(Dates.now()));
        setStart(Dates.minimumDate(Dates.addDays(getEnd(), -1 * range)));
        setAccount(accountService.createInstance());//Instancia de Cuenta
        setOutcome("accounts");
        filter();

        setTreeDataModel(createTreeAccounts());

        setRecord(recordService.createInstance());
        setRecordDetail(recordDetailService.createInstance());

        setRangeReport(-1);
    }

    @Override
    public void handleReturn(SelectEvent event) {
        chargeListDetailsforAccount();
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

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //GETTER AND SETTER
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
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

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Long getParentAccountId() {
        return parentAccountId;
    }

    public void setParentAccountId(Long parentAccountId) {
        this.parentAccountId = parentAccountId;
        if (this.parentAccountId != null && !this.parentAccount.isPersistent()) {
            this.parentAccount = accountService.find(parentAccountId);
        }
    }

    public Account getAccount() {
        if (this.accountId != null && !this.account.isPersistent()) {
            this.account = this.accountCache.lookup(this.accountId);
            this.parentAccount = this.accountCache.lookup(this.account.getParentAccountId());
        }
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public Account getAccountSelected() {
        return this.accountSelected;
    }

    public void setAccountSelected(Account accountSelected) {
        this.accountSelected = accountSelected;
    }

    public Account getParentAccount() {
        if (this.accountId != null && !this.account.isPersistent()) {
            this.parentAccount = accountCache.lookup(this.parentAccountId);
        }
        return this.parentAccount;
    }

    public void setParentAccount(Account parentAccount) {
        this.parentAccount = parentAccount;
    }

    public Account getDepositAccount() {
        return depositAccount;
    }

    public void setDepositAccount(Account depositAccount) {
        this.depositAccount = depositAccount;
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

    public List<Object[]> getObjectsRecordDetail() {
        return objectsRecordDetail;
    }

    public void setObjectsRecordDetail(List<Object[]> objectsRecordDetail) {
        this.objectsRecordDetail = objectsRecordDetail;
    }

    public BigDecimal getAmountAccount() {
        return amountAccount;
    }

    public void setAmountAccount(BigDecimal amountAccount) {
        this.amountAccount = amountAccount;
    }

    public List<RecordDetail> getRecordDetailsAccount() {
        return recordDetailsAccount;
    }

    public void setRecordDetailsAccount(List<RecordDetail> recordDetailsAccount) {
        this.recordDetailsAccount = recordDetailsAccount;
    }

    public Record getRecord() {
        if (this.recordId != null && !this.record.isPersistent()) {
            this.record = recordService.find(recordId);
        }
        return record;
    }

    public void setRecord(Record record) {
        this.record = record;
    }

    public RecordDetail getRecordDetail() {
        return recordDetail;
    }

    public void setRecordDetail(RecordDetail recordDetail) {
        this.recordDetail = recordDetail;
    }

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public BigDecimal getFundAccountMain() {
        if (this.accountSelected != null) {
            this.fundAccountMain = accountService.mayorizar(this.accountSelected, this.organizationData.getOrganization(), getStart(), getEnd());
        }
        return fundAccountMain;
    }

    public void setFundAccountMain(BigDecimal fundAccountMain) {
        this.fundAccountMain = fundAccountMain;
    }

    public List<RecordDetail> getRecordDetailsUpdate() {
        return recordDetailsUpdate;
    }

    public void setRecordDetailsUpdate(List<RecordDetail> recordDetailsUpdate) {
        this.recordDetailsUpdate = recordDetailsUpdate;
    }

    public int getRangeReport() {
        return rangeReport;
    }

    public void setRangeReport(int rangeReport) {
        this.rangeReport = rangeReport;
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // MÉTODOS/FUNCIONES
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Busca objetos <tt>Account</tt> de una organización
     *
     * @return list
     */
    public List<Account> getAccounts() {
        //return accountService.findByOrganization(this.organizationData.getOrganization());
        return accountCache.filterByOrganization(this.organizationData.getOrganization());
    }

    /**
     * Busca objetos <tt>Account</tt> para la clave de búsqueda en las columnas
     * name y code
     *
     * @param keyword
     * @return una lista de objetos <tt>Product</tt> que coinciden con la
     * palabra clave dada.
     */
    public List<Account> find(String keyword) {
        return accountCache.filterByNameOrCode(keyword, this.organizationData.getOrganization()); //sólo cuentas de la organización
    }

    public Account findParentAccount(Account x) {
        return accountCache.lookup(x.getParentAccountId());
    }

    /**
     * Generar el código de un objeto <tt>Account</tt> según su objeto padre
     *
     */
    public void generateNextCode() {
        if (!this.account.isPersistent() && this.parentAccountId != null) {
            //Establecer el padre y el codigo
            this.account.setCode(this.accountCache.genereNextCode(parentAccountId));
            this.account.setParentAccountId(this.parentAccountId);
        }
    }

    /**
     * Manipulación (Busca y filtra) del lazyData de tipo <tt>Account</tt>
     *
     */
    public void clear() {
        filter();
    }

    public void filter() {
        if (lazyDataModel == null) {
            lazyDataModel = new LazyAccountDataModel(accountService);
        }
        // lazyDataModel.setOwner(this.subject);
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

    /**
     * Crea el aŕbol de tipo <tt>Account</tt> para visualización del plan de
     * cuentas de la organización
     *
     * @return treeNode de tipo <tt>Account</tt>
     */
    public TreeNode createTreeAccounts() {
        List<Account> accountsOrder = getAccounts();
        Collections.sort(accountsOrder, (Account account1, Account other) -> account1.getCode().compareToIgnoreCase(other.getCode()));// Ordenar la lista por el atributo getCode()
        TreeNode generalTree = new DefaultTreeNode(new Account(I18nUtil.getMessages("common.code"), I18nUtil.getMessages("common.account")), null); //Árbol general
        TreeNode parent = null;
        for (Account x : accountsOrder) {
            if (x.getParentAccountId() == null) {
                parent = new DefaultTreeNode(x, generalTree);
                addChild(parent, x);
            }

        }
        return generalTree;
    }

    public TreeNode addChild(TreeNode parent, Account accountChild) {
        TreeNode child = null;
        List<Account> childs = accountService.findByNamedQuery("Account.findByParentId", accountChild.getId(), this.organizationData.getOrganization());
        if (childs.isEmpty()) {
            parent.setType("account");
        } else {
            for (Account x : childs) {
                child = new DefaultTreeNode(x, parent);
                addChild(child, x);
            }
        }
        return parent;
    }

    /**
     * Selección de un objeto específico de tipo <tt>Account</tt> para ingresar
     * a su vista
     *
     * @param event
     */
    public void onNodeSelect(NodeSelectEvent event) {
        try {
            if (event != null && event.getTreeNode().getData() != null) {
                Account p = (Account) event.getTreeNode().getData();
                redirectTo("/pages/accounting/account.jsf?accountId=" + p.getId());
            }
        } catch (IOException ex) {
            logger.error("No fue posible seleccionar las {} con nombre {}" + I18nUtil.getMessages("BussinesEntity"), ((BussinesEntity) event.getTreeNode()).getName());
        }
    }

    /**
     * Selección de un objeto específico de tipo <tt>Account</tt> para ingresar
     * a su vista y detalles
     *
     * @param event
     */
    public void onNodeSelectTree(NodeSelectEvent event) {
        try {
            if (event != null && event.getTreeNode().getData() != null) {
                Account p = (Account) event.getTreeNode().getData();
                if (p.getParentAccountId() != null) {
                    redirectTo("/pages/accounting/general_ledger.jsf?accountId=" + p.getParentAccountId());
                } else {
                    redirectTo("/pages/accounting/general_ledger.jsf?accountId=" + p.getId());
                }
            }
        } catch (IOException ex) {
            logger.error("No fue posible seleccionar las {} con nombre {}" + I18nUtil.getMessages("BussinesEntity"), ((BussinesEntity) event.getTreeNode()).getName());
        }
    }

    public void messageTreeNode() {
        this.addWarningMessage(I18nUtil.getMessages("action.warning"), I18nUtil.getMessages("app.fede.accouting.account.report"));
    }

    /**
     * Guardar un nuevo/cambio en el objeto de edición<tt>Account</tt>
     *
     */
    public void save() {
        if (this.parentAccount != null) {
            account.setParentAccountId(this.parentAccount.getId());
        }
        if (account.isPersistent()) {
            account.setLastUpdate(Dates.now());
        } else {
            account.setAuthor(this.subject);
            account.setOwner(this.subject);
            account.setOrganization(this.organizationData.getOrganization());
        }
        accountService.save(account.getId(), account);
        this.accountCache.load(); //recargar todas las cuentas

        setTreeDataModel(createTreeAccounts());//Refrescar el árbol de cuentas
    }

    /**
     * Selección de un objeto específico de tipo <tt>Account</tt> para generar
     * su resumen
     *
     * @param event
     */
    public void onAccountSelect(NodeSelectEvent event) {
        if (event != null && event.getTreeNode().getData() != null && event.getTreeNode().isLeaf()) {
            this.accountSelected = (Account) event.getTreeNode().getData();
            if (accountService.findByNamedQuery("Account.findByParentId", this.accountSelected.getId(), this.organizationData.getOrganization()).isEmpty()) {
                chargeListDetailsforAccount();
            }
        } else {
            this.accountSelected = (Account) event.getTreeNode().getData();
            this.recordDetailsAccount = new ArrayList<>();
            addWarningMessage(I18nUtil.getMessages("action.warning"), I18nUtil.getMessages("app.fede.accouting.ledger.noleaf"));
        }
    }

    /**
     * Calcular el saldo que existe en el detalle de las transacciones
     * realizadas
     *
     */
    private void calculateBalance() {
        BigDecimal accumulativeBalance = new BigDecimal(0);
        for (RecordDetail rd : this.recordDetailsAccount) {
            if (RecordDetail.RecordTDetailType.DEBE.equals(rd.getRecordDetailType())) {
                accumulativeBalance = accumulativeBalance.add(rd.getAmount());
            } else if (RecordDetail.RecordTDetailType.HABER.equals(rd.getRecordDetailType())) {
                accumulativeBalance = accumulativeBalance.subtract(rd.getAmount());
            }
            rd.setAccumulatedBalance(accumulativeBalance);
        }
    }

    /**
     * Buscar los hijos de un objeto<tt>Account</tt>
     *
     * @return treeNode con hijos de un objeto <tt>Account</tt>
     */
    public TreeNode accountChilds() {
        TreeNode parent = new DefaultTreeNode(this.account, null);
        addChild(parent, this.account);
        return parent;
    }

    /**
     * Encontrar los detalles de un objeto <tt>Account</tt> según la
     * modificación de fecha
     *
     */
    public void chargeListDetailsforAccount() {
        setRangeReport(-1);
        setRecordDetailsAccount(this.recordDetailService.findByNamedQuery("RecordDetail.findByAccountAndOrganization", this.accountSelected, Dates.minimumDate(getStart()), Dates.maximumDate(getEnd()), this.organizationData.getOrganization()));
        calculateBalance();
    }

    public void calculeRangeReport() {
        Calendar dayDate = Calendar.getInstance();
        setEnd(Dates.maximumDate(Dates.now()));
        switch (this.rangeReport) {
            case 0://Rango del reporte de la última semana
                setStart(Dates.minimumDate(Dates.addDays(getEnd(), -1 * 7)));
                break;
            case 1://Rango del reporte del último mes
                setStart(Dates.minimumDate(Dates.addDays(getEnd(), -1 * (dayDate.get(Calendar.DAY_OF_MONTH) - 1))));
                break;
            case 2://Rango del reporte del último año
                setStart(Dates.minimumDate(Dates.addDays(getEnd(), -1 * (dayDate.get(Calendar.DAY_OF_YEAR) - 1))));
                break;
        }
        chargeListDetailsforAccount();
    }

    /**
     * Mostrar el formulario para edición del objeto <tt>Record</tt>
     * seleccionado
     *
     * @param recordId
     * @return
     */
    public boolean editarFormularioRecord(Long recordId) {
        if (recordId != null) {
            super.setSessionParameter("recordId", recordId);
        }
        return mostrarFormularioRecord(null);
    }

    public boolean mostrarFormularioRecord(Map<String, List<String>> params) {
        String width = settingHome.getValue(SettingNames.POPUP_WIDTH, "800");
        String height = settingHome.getValue(SettingNames.POPUP_HEIGHT, "600");
        String left = settingHome.getValue(SettingNames.POPUP_LEFT, "0");
        String top = settingHome.getValue(SettingNames.POPUP_TOP, "0");
        super.openDialog(SettingNames.POPUP_FORMULARIO_GENERALLEDGER_RECORD, width, height, left, top, true, params);
        return true;
    }

    public void loadSessionParametersRecord() {
        if (existsSessionParameter("recordId")) {
            this.setRecordId((Long) getSessionParameter("recordId"));
        }
        this.getRecord(); //Carga el objeto persistente
        updateBackUpRecord(); //Respaldar el record original
    }

    public void closeFormularioRecord(Object data) {
        removeSessionParameter("recordId");
        super.closeDialog(data);
    }

    private void updateBackUpRecord() {
        recordDetailsUpdate = new ArrayList<>();
        RecordDetail recordDetailUpdate = recordDetailService.createInstance();
        for (RecordDetail rd : this.record.getRecordDetails()) {
            recordDetailUpdate.setOwner(rd.getOwner());
            recordDetailUpdate.setAccountCode(rd.getAccountCode());
            recordDetailUpdate.setAccountName(rd.getAccountName());
            recordDetailUpdate.setAccountId(rd.getAccountId());
            recordDetailUpdate.setAccount(rd.getAccount());
            recordDetailUpdate.setAmount(rd.getAmount());
            recordDetailUpdate.setRecordDetailType(rd.getRecordDetailType());
            recordDetailsUpdate.add(recordDetailUpdate);
            recordDetailUpdate = recordDetailService.createInstance();
        }
        Collections.sort(recordDetailsUpdate, (RecordDetail recordDetail1, RecordDetail other) -> recordDetail1.getRecordDetailType().compareTo(other.getRecordDetailType()));//Ordenar por tipo de entrada/salida de transacción
    }

    /**
     * El registro actual no esta referenciado a una entidad
     *
     * @return
     */
    public boolean isRecordOfReferen() {
        return this.record.getBussinesEntityId() == null;
    }

    public void messageEditableRecord() {
        if (!isRecordOfReferen()) {
            this.addWarningMessage(I18nUtil.getMessages("action.warning"), I18nUtil.getMessages("app.fede.accounting.record.message.not.editable", " " + this.record.getBussinesEntityType()));
        }
    }

    /**
     * Agrega un detalle al Record
     */
    public void addRecordDetail() {
        if (this.recordDetail.getAccount() != null && (BigDecimal.ZERO.compareTo(this.recordDetail.getAmount()) == -1) && this.recordDetail.getRecordDetailType() != null) {
            this.recordDetail.setOwner(subject);
            this.recordDetailsUpdate.add(this.recordDetail);
            //Preparar para una nueva entrada
            this.recordDetail = recordDetailService.createInstance();
        } else {
            this.addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("app.fede.accounting.recordDetail.incomplete"));
        }
    }

    /**
     * Agrega un record al Journal
     */
    public void saveRecord() {
        BigDecimal sumDebe = new BigDecimal(0);
        BigDecimal sumHaber = new BigDecimal(0);
        for (RecordDetail rd : this.recordDetailsUpdate) {
            if (RecordDetail.RecordTDetailType.DEBE.equals(rd.getRecordDetailType())) {
                sumDebe = sumDebe.add(rd.getAmount());
            } else if (RecordDetail.RecordTDetailType.HABER.equals(rd.getRecordDetailType())) {
                sumHaber = sumHaber.add(rd.getAmount());
            }
        }
        if (sumDebe.compareTo(sumHaber) == 0) {
            for (RecordDetail rd : this.record.getRecordDetails()) {
                rd.setDeleted(true);
            }
            for (RecordDetail rd : this.recordDetailsUpdate) {
                this.record.addRecordDetail(rd);
            }
            recordService.save(this.record.getId(), this.record);
            closeFormularioRecord(record.getId());
        } else {
            this.addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("app.fede.accounting.record.balance.required"));
        }
    }

    @Override
    protected void initializeDateInterval() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Realizar un <tt>Record</tt> para registrar un asiento contable para
     * transferir el dinero de una <tt>Account</tt> a otra
     */
    public void validateDeposit() {
        if (this.depositAccount != null) {
            if (this.amountAccount != null) {
                if (this.amountAccount.compareTo(BigDecimal.ZERO) == 0) {
                    this.addWarningMessage(I18nUtil.getMessages("action.warning"), I18nUtil.getMessages("app.fede.accouting.account.amount"));
                } else {
                    if (this.accountSelected.getId().compareTo(this.depositAccount.getId()) == 0) {
                        this.addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("app.fede.accouting.validate.deposit.account.equals"));
                    } else {
                        registerRecordInJournal();
                        findRecordDetailAccountTop();
                    }
                }
            }
        } else {
            this.addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("app.fede.accouting.validate.deposit.account"));
        }
    }

    public void registerRecordInJournal() {
        GeneralJournal journal = buildFindJournal();//Crear o encontrar el journal y el record, para insertar los recordDetails
        Record record = buildRecord();
        record.addRecordDetail(updateRecordDetail(this.accountSelected.getName()));//Crear/Modificar y anadir un recordDetail al record
        record.addRecordDetail(updateRecordDetail(this.depositAccount.getName()));
        record.setDescription((I18nUtil.getMessages("app.fede.accounting.transfer.from.to", "" + this.accountSelected.getName(), this.depositAccount.getName()).toUpperCase()));
        journal.addRecord(record); //Agregar el record al journal

        journalService.save(journal.getId(), journal); //Guardar el journal

    }

    private GeneralJournal buildFindJournal() {
        GeneralJournal generalJournal = journalService.findUniqueByNamedQuery("GeneralJournal.findByCreatedOnAndOrganization", Dates.minimumDate(Dates.now()), Dates.now(), this.organizationData.getOrganization());
        if (generalJournal == null) {
            generalJournal = journalService.createInstance();
            generalJournal.setOrganization(this.organizationData.getOrganization());
            generalJournal.setOwner(subject);
            generalJournal.setCode(UUID.randomUUID().toString());
            generalJournal.setName(I18nUtil.getMessages("app.fede.accounting.journal.properties", "" + this.organizationData.getOrganization().getInitials(), Dates.toDateString(Dates.now())));
            journalService.save(generalJournal); //Guardar el journal creado
            generalJournal = journalService.findUniqueByNamedQuery("GeneralJournal.findByCreatedOnAndOrganization", Dates.minimumDate(Dates.now()), Dates.now(), this.organizationData.getOrganization());
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

    @Override
    public Record aplicarReglaNegocio(String nombreRegla, Object fuenteDatos) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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

    //LIBRO MAYOR DE PLAN DE CUENTAS
    public boolean mostrarFormularioLedgerValues(Map<String, List<String>> params) {
        String width = settingHome.getValue(SettingNames.POPUP_WIDTH, "800");
        String height = settingHome.getValue(SettingNames.POPUP_HEIGHT, "600");
        String left = settingHome.getValue(SettingNames.POPUP_LEFT, "0");
        String top = settingHome.getValue(SettingNames.POPUP_TOP, "0");
        super.openDialog(SettingNames.POPUP_FORMULARIO_GENERALLEDGER, width, height, left, top, true, params);
        return true;
    }

    public boolean mostrarFormularioLedger(Account account) {
        if (account != null) {
            this.accountSelected = account;
        }
        if (this.accountSelected != null) {
            super.setSessionParameter("account", this.accountSelected);
        }

        return mostrarFormularioLedgerValues(null);
    }

    public void closeFormularioLedger(Object data) {
        removeSessionParameter("account");
        super.closeDialog(data);
    }

    public void loadSessionParameters() {
        if (existsSessionParameter("account")) {
            this.setAccountSelected((Account) getSessionParameter("account"));
            this.getAccountSelected(); //Carga el objeto persistente
        }
    }

}
