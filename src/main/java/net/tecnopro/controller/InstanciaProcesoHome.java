/*
 * Copyright (C) 2016 jlgranda
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
package net.tecnopro.controller;

import com.google.common.base.Strings;
import com.jlgranda.fede.SettingNames;
import com.jlgranda.fede.ejb.GroupService;
import com.jlgranda.fede.ejb.SerialService;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.inject.Named;
import javax.faces.view.ViewScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import net.tecnopro.document.ejb.DocumentoService;
import net.tecnopro.document.ejb.InstanciaProcesoService;
import net.tecnopro.document.ejb.TareaService;
import net.tecnopro.document.model.Documento;
import net.tecnopro.document.model.EstadoTipo;
import net.tecnopro.document.model.InstanciaProceso;
import net.tecnopro.document.model.ProcesoTipo;
import net.tecnopro.document.model.Tarea;
import org.jlgranda.fede.controller.FedeController;
import org.jlgranda.fede.controller.OrganizationData;
import org.jlgranda.fede.controller.SettingHome;
import org.jlgranda.fede.model.accounting.Record;
import org.jlgranda.fede.model.document.DocumentType;
import org.jlgranda.fede.ui.model.LazyInstanciaProcesoDataModel;
import org.jpapi.model.BussinesEntity;
import org.jpapi.model.Group;
import org.jpapi.model.Organization;
import org.jpapi.model.StatusType;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.Dates;
import org.jpapi.util.I18nUtil;
import org.jpapi.util.Lists;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.UnselectEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.file.UploadedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jlgranda
 */
@ViewScoped
@Named
public class InstanciaProcesoHome extends FedeController implements Serializable {

    private static final long serialVersionUID = -2712214748501882991L;

    Logger logger = LoggerFactory.getLogger(InstanciaProcesoHome.class);
   
    @Inject
    private OrganizationData organizationData;

    @Inject
    private Subject subject;

    private Long instanciaProcesoId;

    private InstanciaProceso instanciaProceso;

    @Inject
    private SettingHome settingHome;

    @EJB
    private GroupService groupService;

    @EJB
    private InstanciaProcesoService instanciaProcesoService;
    
    @EJB
    private TareaService tareaService;

    private Tarea tarea; //La tarea en edición
    private Tarea tareaSeleccionada; //La tarea en edición

    //private List<Group> groups = new ArrayList<>();
    private Documento documento;
    private Subject solicitante;

    private Subject destinatario;

    private LazyInstanciaProcesoDataModel lazyDataModel;

    @EJB
    private DocumentoService documentoService;

    private List<Documento> documentosRemovidos = new ArrayList<>();

    //UI variables
    private String activeIndex;
    private String keys;
    
    @Inject
    private TareaHome tareaHome;
    
    private String accion;
    
    private boolean usarDestinatarioPorDefecto;
    
    private StatusType statusType;

    @PostConstruct
    public void init() {
        int amount = 0;
        try {
            amount = Integer.valueOf(settingHome.getValue(SettingNames.DASHBOARD_RANGE, "30"));
        } catch (java.lang.NumberFormatException nfe) {
            amount = 30;
        }

        setEnd(Dates.now()); //Hoy más un día para que incluya lo creados hoy
        setStart(Dates.addDays(getEnd(), -1 * amount));
        setOutcome("procesos");

        setInstanciaProceso(instanciaProcesoService.createInstance());
        setTarea(tareaService.createInstance()); //Siempre listo para recibir la respuesta del proceso
        setDocumento(documentoService.createInstance());
        
        //Inicializar solicitante al usuario logeado
        this.actualizarSolicitantePorDefecto();
    }

    public Long getInstanciaProcesoId() {
        return instanciaProcesoId;
    }

    public void setInstanciaProcesoId(Long instanciaProcesoId) {
        this.instanciaProcesoId = instanciaProcesoId;
    }

    public InstanciaProceso getInstanciaProceso() {
        if (this.instanciaProcesoId != null && !this.instanciaProceso.isPersistent()) {
            this.instanciaProceso = instanciaProcesoService.find(instanciaProcesoId);
        }
        return instanciaProceso;
    }

    public boolean isUsarDestinatarioPorDefecto() {
        return usarDestinatarioPorDefecto;
    }

    public void setUsarDestinatarioPorDefecto(boolean usarDestinatarioPorDefecto) {
        this.usarDestinatarioPorDefecto = usarDestinatarioPorDefecto;
    }

    public void actualizarSolicitantePorDefecto() {
        this.setSolicitante(this.subject);
    }
    
    public void actualizarDestinatarioPorDefecto() {
        if (this.getDestinatario() == null){
            this.setDestinatario(this.subject);
        } else {
            this.setDestinatario(null);
        }
    }

    public StatusType getStatusType() {
        return statusType;
    }

    public void setStatusType(StatusType statusType) {
        this.statusType = statusType;
    }
    
    public boolean mostrarFormularioNuevaEtiqueta() {
        String width = settingHome.getValue(SettingNames.POPUP_SMALL_WIDTH, "400");
        String height = settingHome.getValue(SettingNames.POPUP_SMALL_HEIGHT, "240");
        super.openDialog(SettingNames.POPUP_NUEVA_ETIQUETA, width, height, true);
        return true;
    }

    public void setInstanciaProceso(InstanciaProceso instanciaProceso) {
        this.instanciaProceso = instanciaProceso;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public Tarea getTarea() {
        return tarea;
    }

    public void setTarea(Tarea tarea) {
        this.tarea = tarea;
    }

    public Tarea getTareaSeleccionada() {
        return tareaSeleccionada;
    }

    public void setTareaSeleccionada(Tarea tareaSeleccionada) {
        this.tareaSeleccionada = tareaSeleccionada;
    }

    public Subject getSolicitante() {
        return solicitante;
    }

    public void setSolicitante(Subject solicitante) {
        this.solicitante = solicitante;
    }

    public Subject getDestinatario() {
        return destinatario;
    }

    public void setDestinatario(Subject destinatario) {
        this.destinatario = destinatario;
    }

    public String getAccion() {
        return accion;
    }

    public void setAccion(String accion) {
        this.accion = accion;
    }
    

    /**
     * Persistencia de la instancia de proceso
     */
    public void save() {
        if (getDestinatario() == null || getSolicitante() == null){
            addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("error.task.persons"));
            return;
        }
        try {
            if (!this.tarea.isPersistent()) {//Id nulo, es tarea nueva
                //Crear proceso y asignar a tarea
                this.instanciaProceso.setCode(SerialService.getGenerator().next()); //Crear un generador de Process ID
                this.instanciaProceso.setAuthor(subject);
                this.instanciaProceso.setProcesoTipo(ProcesoTipo.NEGOCIO);
                this.instanciaProceso.setOwner(getSolicitante()); //El solicitante del proceso o tramite
                this.instanciaProceso.setOrganization(this.organizationData.getOrganization()); //La organización seleccionada

                instanciaProcesoService.save(this.instanciaProceso.getId(), this.instanciaProceso);   
                
                //2. Siguiente tarea
                getTarea().setName(this.instanciaProceso.getName());
                getTarea().setDescription(this.instanciaProceso.getDescription());
                
                Tarea next = buildTarea( this.tarea, this.organizationData.getOrganization(), subject, getDestinatario(), this.instanciaProceso);
                
                getTarea().getDocumentos().stream().forEach((doc) -> {
                    next.addDocumento(doc);
                });
                
                procesarDocumentos(next);
                
                tareaService.save(next.getId(), next);
                
                //Enviar notificación de inicio de proceso
                tareaHome.sendNotification(this.instanciaProceso, "app.mail.template.process.start", false);
                //Enviar notificación de tarea por realizar
                tareaHome.sendNotification(next, "app.mail.template.task.assign", false);
                setOutcome("procesos");
            } else {
                
                //Actualizar destinatario si hay cambios.
                //TODO  notificar al destinatario anterior y nuevo
                Subject temp = null;
                if (!getTarea().getOwner().equals(getDestinatario())){
                    temp = getTarea().getOwner();
                    getTarea().setOwner(getDestinatario());
                }
                tareaService.save(getTarea().getId(), getTarea());
                procesarDocumentos(getTarea());
                eliminarDocumentos();
                
                //Enviar notificación de modificación de tarea
                tareaHome.sendNotification(getTarea(), "app.mail.template.task.assign", false);
            }
            this.addDefaultSuccessMessage();
        } catch (Exception e) {
            addErrorMessage(e, I18nUtil.getMessages("error.persistence"));
        }
    }
    
    public void send() {
        try {
            
            //1. Actualizar el estado de la tarea seleccionada al presionar responder/reenviar
            getTareaSeleccionada().setEstadoTipo(EstadoTipo.RESUELTO);
            
            //2. Crear siguiente tarea en base a lo recolectado en la pantalla
            Tarea next = buildTarea(getTarea(), this.organizationData.getOrganization(), subject, getDestinatario(), this.getInstanciaProceso());
            getTarea().getDocumentos().forEach(doc -> {
                next.addDocumento(doc);
            });
            procesarDocumentos(next); //Relacionar documentos a la tarea
            eliminarDocumentos(); //Procesar documentos eliminados de la tarea
            //Guardar cambios
            getInstanciaProceso().addTarea(next);
            
            if (EstadoTipo.CERRADO.equals(next.getEstadoTipo())){
                instanciaProceso.setStatus(StatusType.CLOSE.toString()); //Cerrar el proceso si la tarea es la de cierre
            }

            instanciaProcesoService.save(getInstanciaProceso().getId(), getInstanciaProceso());

            //Encerar tarea para recoger nueva respuesta
            setTarea(tareaService.createInstance());
            setDestinatario(null);
            setActiveIndex(null);
            
            //Enviar notificación de tarea completada y por completar
            tareaHome.sendNotification(getTareaSeleccionada(), "app.mail.template.task.done", false);
            tareaHome.sendNotification(next, "app.mail.template.task.assign", false, true); //Forzar ya que next no tiene ID
            this.addDefaultSuccessMessage();
        } catch (Exception e) {
            addErrorMessage(e, I18nUtil.getMessages("error.persistence"));
        }
    }

    private Tarea buildTarea(Tarea tarea, Organization organization, Subject author, Subject owner, InstanciaProceso instanciaProceso) {
        //2. Siguiente tarea
        Tarea _tarea = tareaService.createInstance();
        _tarea.setName(tarea.getName());
        _tarea.setDescription(tarea.getDescription());
        _tarea.setInstanciaProceso(instanciaProceso);
        //Es temporral hasta que se pueda seleccionar una organización
        _tarea.setDepartamento("temporal");
        _tarea.setOrganization(organization == null ? instanciaProceso.getOrganization() : organization);
        _tarea.setAuthor(author); //usuario logeado
        _tarea.setOwner(owner); //destinatario
        _tarea.setEstadoTipo(tarea.getEstadoTipo() == null ? EstadoTipo.ESPERA : tarea.getEstadoTipo());//La tarea se completa al iniciar el proceso
        return _tarea;
    }

    public void handleFileUpload(FileUploadEvent event) {
        procesarUploadFile(event.getFile());
    }

    public void procesarUploadFile(UploadedFile file) {
        if (file == null) {
            this.addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("app.fede.fedecard.file.null"));
            return;
        }

        if (subject == null) {
            this.addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("app.signin.login.user.null"));
            return;
        }
        try {
            Documento doc = crearDocumento(file);
            
            if (getTarea() != null && doc != null) {
                getTarea().getDocumentos().add(doc);
            } else {
                this.addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("error.nulls"));
            }
            //Encerar el obeto para edición de nuevo documento
            setDocumento(documentoService.createInstance());
        } catch (Exception e) {
            this.addErrorMessage(I18nUtil.getMessages("action.fail"), e.getMessage());
        }
    }

    public void procesarDocumentos(Tarea t) {
        t.getDocumentos().stream().forEach((doc) -> {
            generaDocumento(new File(doc.getRuta()), doc.getContents());
        });
    }

    public StreamedContent downloadDocument(Documento doc) {
        StreamedContent fileDownload = null;
        try {
            if (doc != null) {
                String contentType = doc.getMimeType();
                String name = doc.getName();
                InputStream is = new FileInputStream(new File(doc.getRuta()));
                fileDownload = DefaultStreamedContent.builder().contentType(contentType).name(name).stream(() -> is).build();
            }
        } catch (FileNotFoundException e) {
        }
        return fileDownload;
    }

    public void eliminarDocumentos() {
        documentosRemovidos.stream().map(doc -> {
            if (doc.isPersistent()) {
                documentoService.remove(doc.getId(), doc);
            }
            return doc;
        }).forEachOrdered(doc -> {
            documentosRemovidos.remove(doc);
        });
    }

    public void removerDocumento(Documento doc, Tarea t) {
        this.documentosRemovidos.add(doc);
        t.getDocumentos().remove(doc);
    }

    public void generaDocumento(File file, byte[] bytes) {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            try (BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                bos.write(bytes);
                bos.flush();
            }

        } catch (IOException ex) {
            addErrorMessage(ex, I18nUtil.getMessages("common.error.upload"));
        }
    }

    public void setActiveIndex(String activeIndex) {
        this.activeIndex = activeIndex;
    }

    /**
     * Calcula los indices de los tabs a mostrar expandidos, en función del
     * estado de las tareas de la instancia de proceso
     *
     * @return lista de indices separadas por coma
     */
    public String getActiveIndex() {
        if (Strings.isNullOrEmpty(activeIndex)) {
            if (getInstanciaProceso() == null) {
                activeIndex = "";
            } else {
                List<Integer> indexs = new ArrayList<>();
                int i = 0;
                for (Tarea t : getInstanciaProceso().getTareas()) {
                    if (EstadoTipo.ESPERA.equals(t.getEstadoTipo())) {
                        indexs.add(i++);
                    }
                }
                activeIndex = Lists.toString(indexs).trim();
            }
        }

        return activeIndex;
    }

    private Documento crearDocumento(UploadedFile file) {
        
        Documento doc = documentoService.createInstance();
        doc.setOwner(subject);
        doc.setAuthor(subject);
        doc.setOrganization(this.organizationData.getOrganization());
        
        if (getDocumento() != null) {
            doc.setName(Strings.isNullOrEmpty(getDocumento().getName()) ? file.getFileName() : getDocumento().getName());
            doc.setDocumentType(getDocumento().getDocumentType() == null ? DocumentType.UNDEFINED : getDocumento().getDocumentType());
        } else {
            this.addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("app.fede.fedecard.file.null"));
            return null;
        }
        doc.setFileName(file.getFileName());
        doc.setNumeroRegistro(UUID.randomUUID().toString());

        doc.setRuta(settingHome.getValue("app.management.tarea.documentos.ruta", "/tmp") + "//" + file.getFileName());

        doc.setMimeType(file.getContentType());

        /**
         * Permite que el documento tenga asignado los bytes para posteriormete
         * con dichos bytes generar el documento digital y guardarlo en la ruta
         * definida
         */
        doc.setContents(file.getContent());
        return doc;
    }

    public Documento getDocumento() {
        return documento;
    }

    private Group findGroup(String key) {
        for (Group g : getGroups()) {
            if (key.equalsIgnoreCase(g.getCode())) {
                return g;
            }
        }
        return new Group("null", "null");
    }

    public void setDocumento(Documento documento) {
        this.documento = documento;
    }

    public List<Documento> getDocumentosRemovidos() {
        return documentosRemovidos;
    }

    public void setDocumentosRemovidos(List<Documento> documentosRemovidos) {
        this.documentosRemovidos = documentosRemovidos;
    }

    @Override
    public void handleReturn(SelectEvent event) {

    }

    @Override
    public Group getDefaultGroup() {
        return this.defaultGroup;
    }

    public void filter() {
        if (lazyDataModel == null) {
            lazyDataModel = new LazyInstanciaProcesoDataModel(instanciaProcesoService);
        }

        lazyDataModel.setOrganization(this.organizationData.getOrganization());
        //lazyDataModel.setOwner(subject);
        lazyDataModel.setStart(getStart());
        lazyDataModel.setEnd(getEnd());
        lazyDataModel.setStatusType(getStatusType());

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

    public void onRowSelect(SelectEvent event) throws IOException {
        //Redireccionar a RIDE de objeto seleccionado
        if (event != null && event.getObject() != null) {
            redirectTo("/pages/management/proceso/instancia_proceso.jsf?instanciaProcesoId=" + ((InstanciaProceso) event.getObject()).getId());
        }
    }

    public void onRowUnselect(UnselectEvent event) {
        FacesMessage msg = new FacesMessage(I18nUtil.getMessages("BussinesEntity") + " " + I18nUtil.getMessages("common.unselected"), ((BussinesEntity) event.getObject()).getName());

        FacesContext.getCurrentInstance().addMessage(null, msg);
        this.selectedBussinesEntities.remove((InstanciaProceso) event.getObject());
        logger.info(I18nUtil.getMessages("BussinesEntity") + " " + I18nUtil.getMessages("common.unselected"), ((BussinesEntity) event.getObject()).getName());
    }

    public String getKeys() {
        return keys;
    }

    public void setKeys(String keys) {
        this.keys = keys;
    }

    public String getSelectedKeys() {
        String _keys = "";
        if (getSelectedBussinesEntities() != null && !getSelectedBussinesEntities().isEmpty()) {
            _keys = Lists.toString(getSelectedBussinesEntities());
        }
        return _keys;
    }

    public LazyInstanciaProcesoDataModel getLazyDataModel() {

        filter();

        return lazyDataModel;
    }

    public void setLazyDataModel(LazyInstanciaProcesoDataModel lazyDataModel) {
        this.lazyDataModel = lazyDataModel;
    }

    @Override
    public List<Group> getGroups() {
        if (groups.isEmpty()) {
            groups = groupService.findByOwnerAndModuleAndType(subject, "documents", Group.Type.LABEL);
        }

        return groups;
    }

    @Override
    public void setGroups(List<Group> groups) {
        this.groups = groups;
    }

    @Override
    protected void initializeDateInterval() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * Preparar formulario de respuesta
     * @param estadoTipo
     * @param tarea 
     */
    
    public void prepararAccion(EstadoTipo estadoTipo, Tarea tarea){
        //La tarea que esta siendo respondida/reenviada
        setTareaSeleccionada(tarea);
        
        if (tarea == null){
            setTarea(tareaService.createInstance());
        }
        
        getTarea().setId(tarea.getId()); //El id de la tarea a responder
        
        switch (estadoTipo) {
            case ACCION_RESPONDER:
                //Preinicializar la tarea respuesta
                setDestinatario(tarea.getAuthor());
                getTarea().setName(settingHome.getValue(SettingNames.DOCUMENTS_REPLY, "RE:").concat(" ").concat(getTareaSeleccionada().getName()));
                getTarea().setEstadoTipo(EstadoTipo.ESPERA);
                break;
            case ACCION_REDIRECCIONAR:
                setDestinatario(null);
                getTarea().setName(settingHome.getValue(SettingNames.DOCUMENTS_FORWARD, "FW:").concat(" ").concat(getTareaSeleccionada().getName()));
                getTarea().setEstadoTipo(EstadoTipo.ESPERA);
                break;
            case ACCION_FINALIZAR:
                setDestinatario(tarea.getInstanciaProceso().getOwner());
                getTarea().setName(settingHome.getValue(SettingNames.DOCUMENTS_END, "Finalizar:").concat(" ").concat(getTareaSeleccionada().getName()));
                getTarea().setEstadoTipo(EstadoTipo.CERRADO);
                break;
            default:
                break;
        }
    }

    @Override
    public Record aplicarReglaNegocio(String nombreRegla, Object fuenteDatos) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
