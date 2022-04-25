/*
 * Copyright (C) 2022 usuario
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
import com.jlgranda.fede.ejb.RecordDetailService;
import com.jlgranda.fede.ejb.accounting.AccountCache;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.model.SelectItem;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.jlgranda.fede.model.accounting.Account;
import org.jlgranda.fede.model.accounting.Record;
import org.jlgranda.fede.model.accounting.RecordDetail;
import org.jpapi.model.Group;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.Dates;
import org.jpapi.util.I18nUtil;
import org.primefaces.event.NodeSelectEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

/**
 *
 * @author usuario
 */
@Named
@ViewScoped
public class GeneralLedgerHome extends FedeController implements Serializable {

    @Inject
    private Subject subject;
    @Inject
    private OrganizationData organizationData;
    @Inject
    private SettingHome settingHome;

    @EJB
    AccountCache accountCache;
    @EJB
    private AccountService accountService;
    @EJB
    private RecordDetailService recordDetailService;

    /**
     * UX.
     */
    private List<Account> accountsCatalogue;
    private TreeNode treeDataModel;
    private TreeNode singleSelectedNode;

    private Account accountSelected;
    private BigDecimal accountSelectedFundTotal;//Saldo de la cuenta seleccionada desde el 1 registro hasta hoy
    private BigDecimal accountSelectedDebePartial;//Saldo de la cuenta seleccionada desde el 1 registro hasta la fecha end
    private BigDecimal accountSelectedHaberPartial;//Saldo de la cuenta seleccionada desde el 1 registro hasta antes de la fecha end
    private BigDecimal accountSelectedFundPartial;//Saldo de la cuenta seleccionada desde el 1 registro hasta antes de la fecha end
    private BigDecimal accountSelectedFundPrevious;//Saldo de la cuenta seleccionada desde el 1 registro hasta antes de la fecha start
    private List<RecordDetail> accountSelectedRecords;

    private int rangeId;

    @PostConstruct
    private void init() {
        setEnd(Dates.maximumDate(Dates.now()));
        setStart(Dates.minimumDate(Dates.addDays(getEnd(), -1 * (Dates.getDayOfMonth(getEnd()) - 1))));

        setRangeId(1);
        initVariablesSummary();
        setAccountsCatalogue(accountCache.filterByOrganization(this.organizationData.getOrganization()));
        setTreeDataModel(getAccountsCatalogueTree());

        setOutcome("general-ledger");
    }

    /**
     * METHODS UTIL.
     */
    public TreeNode getAccountsCatalogueTree() {

        List<Account> accountsRoot = new ArrayList<>();
        Map<Long, Account> accountsMap = new HashMap<>();

        this.accountsCatalogue.forEach(acc -> {
            if (acc.getParentAccountId() == null) {
                accountsRoot.add(acc);
                Collections.sort(accountsRoot, (Account account1, Account other) -> account1.getCode().compareToIgnoreCase(other.getCode()));// Ordenar la lista por el atributo getCode()
            } else {
                accountsMap.put(acc.getId(), acc);
            }
        });

        TreeNode generalTree = new DefaultTreeNode();
        if (!accountsRoot.isEmpty()) {
            for (Account acc : accountsRoot) {
                TreeNode node = new DefaultTreeNode(acc, generalTree);
                generatedNodesDown(node, accountsMap, encontrarPorParentAccountEnMapa(acc.getId(), accountsMap));
                generalTree.getChildren().add(node);
            }
        } else {
            TreeNode node = new DefaultTreeNode(new Account(I18nUtil.getMessages(""), "Aún sin definir el Plan de Cuentas de la Organización"), generalTree);
            node.setType("empty");
            generalTree.getChildren().add(node);
            generalTree.setExpanded(Boolean.FALSE);
            generalTree.setSelectable(Boolean.FALSE);
        }
        return generalTree;
    }

    private void generatedNodesDown(TreeNode parent, Map<Long, Account> accountsMap, List<Account> accountsDown) {
        TreeNode data = null;
        for (Account acc : accountsDown) {
            TreeNode node = new DefaultTreeNode(acc, parent);
            List<Account> accountsChild = encontrarPorParentAccountEnMapa(acc.getId(), accountsMap);
            node.setType(accountsChild.isEmpty() ? "sheet" : "default");
            generatedNodesDown(node, accountsMap, accountsChild);
            parent.getChildren().add(node);
        }
    }

    private List<Account> encontrarPorParentAccountEnMapa(Long accountId, Map<Long, Account> accountsMap) {
        List<Account> accountsDown = new ArrayList<>();
        accountsMap.forEach((key, data) -> {
            if (accountId.equals(data.getParentAccountId())) {
                accountsDown.add(data);
            }
        });
        Collections.sort(accountsDown, (Account account1, Account other) -> account1.getCode().compareToIgnoreCase(other.getCode()));// Ordenar la lista por el atributo getCode()
        return accountsDown;
    }

    public void onNodeSelected(NodeSelectEvent event) {
        if (event.getTreeNode() != null && event.getTreeNode().getData() != null) {
            setAccountSelected((Account) event.getTreeNode().getData());
            getSummaryRecordByAccount();
        } else {
            initVariablesSummary();
        }
    }

    public void getSummaryRecordByAccount() {
        setRangeId(-1);
        initVariablesSummary();
        setAccountSelectedFundTotal(accountService.mayorizarTo(getAccountSelected(), this.organizationData.getOrganization(), Dates.maximumDate(Dates.now())));
        setAccountSelectedDebePartial(accountService.mayorizarPorTipo(RecordDetail.RecordTDetailType.DEBE, getAccountSelected(), this.organizationData.getOrganization(), Dates.minimumDate(getStart()), Dates.maximumDate(getEnd())));
        setAccountSelectedHaberPartial(accountService.mayorizarPorTipo(RecordDetail.RecordTDetailType.HABER, getAccountSelected(), this.organizationData.getOrganization(), Dates.minimumDate(getStart()), Dates.maximumDate(getEnd())));
        setAccountSelectedFundPartial(getAccountSelectedDebePartial().subtract(getAccountSelectedHaberPartial()));
        setAccountSelectedFundPrevious(accountService.mayorizarTo(getAccountSelected(), this.organizationData.getOrganization(), Dates.maximumDate(Dates.addDays(getStart(), -1 * 1))));
        setAccountSelectedRecords(recordDetailService.findByNamedQuery("RecordDetail.findByAccountAndEmissionOnAndOrganization", getAccountSelected(), Dates.minimumDate(getStart()), Dates.maximumDate(getEnd()), this.organizationData.getOrganization()));
        calculateBalance();
    }

    private void initVariablesSummary() {
        setAccountSelectedFundTotal(BigDecimal.ZERO);
        setAccountSelectedDebePartial(BigDecimal.ZERO);
        setAccountSelectedHaberPartial(BigDecimal.ZERO);
        setAccountSelectedFundPartial(BigDecimal.ZERO);
        setAccountSelectedRecords(new ArrayList<>());
    }

    private void calculateBalance() {
        BigDecimal accumulativeBalance = BigDecimal.ZERO;
        accumulativeBalance = accumulativeBalance.add(getAccountSelectedFundPrevious());
        for (RecordDetail rd : getAccountSelectedRecords()) {
            if (RecordDetail.RecordTDetailType.DEBE.equals(rd.getRecordDetailType())) {
                accumulativeBalance = accumulativeBalance.add(rd.getAmount());
            } else if (RecordDetail.RecordTDetailType.HABER.equals(rd.getRecordDetailType())) {
                accumulativeBalance = accumulativeBalance.subtract(rd.getAmount());
            }
            rd.setAccumulatedBalance(accumulativeBalance);
        }
    }

    public void getRangeOfRecords() {
        setEnd(Dates.now());
        switch (getRangeId()) {
            case 0://Rango del reporte de la última semana
                setStart(Dates.minimumDate(Dates.addDays(getEnd(), -1 * (Dates.getDayOfWeek(getEnd()) - 2))));
                break;
            case 1://Rango del reporte del último mes
                setStart(Dates.minimumDate(Dates.addDays(getEnd(), -1 * (Dates.getDayOfMonth(getEnd()) - 1))));
                break;
            case 2://Rango del reporte del último año
                setStart(Dates.minimumDate(Dates.addDays(getEnd(), -1 * (Dates.getDayOfYear(getEnd()) - 1))));
                break;
        }
        getSummaryRecordByAccount();
    }

    public boolean editarFormularioRecord(Long recordId) {
        if (recordId != null) {
            super.setSessionParameter("recordId", recordId);
        }
        return mostrarFormularioRecord(null);
    }

    public boolean mostrarFormularioRecord(Map<String, List<String>> params) {
        String width = settingHome.getValue(SettingNames.POPUP_WIDTH, "800");
        String height = settingHome.getValue(SettingNames.POPUP_HEIGHT, "400");
        String left = settingHome.getValue(SettingNames.POPUP_LEFT, "0");
        String top = settingHome.getValue(SettingNames.POPUP_TOP, "0");
        super.openDialog(SettingNames.POPUP_FORMULARIO_GENERALLEDGER_RECORD, width, height, left, top, true, params);
        return true;
    }

    public List<Account> getAccountsCatalogue() {
        return accountsCatalogue;
    }

    public void setAccountsCatalogue(List<Account> accountsCatalogue) {
        this.accountsCatalogue = accountsCatalogue;
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

    public Account getAccountSelected() {
        return accountSelected;
    }

    public void setAccountSelected(Account accountSelected) {
        this.accountSelected = accountSelected;
    }

    public BigDecimal getAccountSelectedFundTotal() {
        return accountSelectedFundTotal;
    }

    public void setAccountSelectedFundTotal(BigDecimal accountSelectedFundTotal) {
        this.accountSelectedFundTotal = accountSelectedFundTotal;
    }

    public BigDecimal getAccountSelectedDebePartial() {
        return accountSelectedDebePartial;
    }

    public void setAccountSelectedDebePartial(BigDecimal accountSelectedDebePartial) {
        this.accountSelectedDebePartial = accountSelectedDebePartial;
    }

    public BigDecimal getAccountSelectedHaberPartial() {
        return accountSelectedHaberPartial;
    }

    public void setAccountSelectedHaberPartial(BigDecimal accountSelectedHaberPartial) {
        this.accountSelectedHaberPartial = accountSelectedHaberPartial;
    }

    public BigDecimal getAccountSelectedFundPartial() {
        return accountSelectedFundPartial;
    }

    public void setAccountSelectedFundPartial(BigDecimal accountSelectedFundPartial) {
        this.accountSelectedFundPartial = accountSelectedFundPartial;
    }

    public BigDecimal getAccountSelectedFundPrevious() {
        return accountSelectedFundPrevious;
    }

    public void setAccountSelectedFundPrevious(BigDecimal accountSelectedFundPrevious) {
        this.accountSelectedFundPrevious = accountSelectedFundPrevious;
    }

    public List<RecordDetail> getAccountSelectedRecords() {
        return accountSelectedRecords;
    }

    public void setAccountSelectedRecords(List<RecordDetail> accountSelectedRecords) {
        this.accountSelectedRecords = accountSelectedRecords;
    }

    public int getRangeId() {
        return rangeId;
    }

    public void setRangeId(int rangeId) {
        this.rangeId = rangeId;
    }

    @Override
    public void handleReturn(SelectEvent event) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Record aplicarReglaNegocio(String nombreRegla, Object fuenteDatos) {
        Record record = null;
        //El registro casí listo para agregar al journal
        return record;
    }

    @Override
    public Group getDefaultGroup() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void initializeDateInterval() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Group> getGroups() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
