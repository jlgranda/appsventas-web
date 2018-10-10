/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.tecnopro.controller;

import com.google.common.base.Strings;
import com.jlgranda.fede.SettingNames;
import com.jlgranda.fede.ejb.GroupService;
import com.jlgranda.fede.ejb.SerialService;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
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
import org.jlgranda.fede.cdi.LoggedIn;
import org.jlgranda.fede.controller.FacturaElectronicaHome;
import org.jlgranda.fede.controller.FedeController;
import org.jlgranda.fede.controller.OrganizationHome;
import org.jlgranda.fede.controller.SettingHome;
import org.jlgranda.fede.model.document.DocumentType;
import org.jlgranda.fede.ui.model.LazyTareaDataModel;
import org.jpapi.model.BussinesEntity;
import org.jpapi.model.Group;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.I18nUtil;
import org.primefaces.context.RequestContext;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.UnselectEvent;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.beanutils.BeanUtils;
import org.jlgranda.fede.controller.admin.TemplateHome;
import org.jpapi.util.Dates;
import org.primefaces.model.UploadedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

/**
 *
 * @author Jorge
 */
@ViewScoped
@Named("tareaHome")
public class TareaHome extends FedeController implements Serializable {

    private static final long serialVersionUID = 439606517163781752L;

    Logger logger = LoggerFactory.getLogger(TareaHome.class);

    private Subject subject;

    private Subject solicitante;

    private Subject destinatario;

    @Inject
    private SettingHome settingHome;
    
    @EJB
    private InstanciaProcesoService procesoService;

    @EJB
    private TareaService tareaService;
    
    @EJB
    private GroupService groupService;

    private List<Tarea> ultimasTareas = new ArrayList<>();
    private List<Tarea> misUltimasTareasEnviadas = new ArrayList<>();
    private List<Tarea> misUltimasTareasRecibidas = new ArrayList<>();
    private List<Documento> documentosRemovidos = new ArrayList<>();
    /**
     * Entidad para edición
     */
    private Tarea tarea;
    private InstanciaProceso proceso;
    private Tarea siguienteTarea;
    private Tarea ultimaTareaRecibida;
    private Tarea ultimaTareaEnviada;
    private String estado;
    private Tarea selectedTarea;
    private Documento documento;
    private Documento documentoAceptar;
    private Long documentoId;

    private Long tareaId;
    private Long procesoId;

    @EJB
    private DocumentoService documentoService;
    /**
     * Controla el comportamiento del controlador y pantalla
     */
    private LazyTareaDataModel lazyDataModel;

    @Inject
    private OrganizationHome organizationHome;
    
    @Inject
    private TemplateHome templateHome;

    @PostConstruct
    public void init() {
        setTarea(tareaService.createInstance());
        setSiguienteTarea(tareaService.createInstance());
        setDocumento(documentoService.createInstance());

        int amount = 0;
        try {
            amount = Integer.valueOf(settingHome.getValue(SettingNames.DASHBOARD_RANGE, "360"));
        } catch (java.lang.NumberFormatException nfe) {
            amount = 30;
        }

        setEnd(Dates.now());
        setStart(Dates.addDays(getEnd(), -1 * amount));

        setOutcome("tareas");

        //TODO Establecer temporalmente la organización por defecto
        //getOrganizationHome().setOrganization(organizationService.find(1L));
    }

    public Tarea getUltimaTareaRecibida() {
        if (ultimaTareaRecibida == null) {
            List<Tarea> obs = tareaService.findByNamedQuery("Tarea.findLastsByOwner", subject, EstadoTipo.ESPERA);
            ultimaTareaRecibida = obs.isEmpty() ? new Tarea() : (Tarea) obs.get(0);
        }
        return ultimaTareaRecibida;
    }

    public void setUltimaTareaRecibida(Tarea ultimaTareaRecibida) {
        this.ultimaTareaRecibida = ultimaTareaRecibida;
    }

    public Tarea getUltimaTareaEnviada() {
        if (ultimaTareaEnviada == null) {
            List<Tarea> obs = tareaService.findByNamedQuery("Tarea.findLastsByAuthor", subject);
            ultimaTareaEnviada = obs.isEmpty() ? new Tarea() : (Tarea) obs.get(0);
        }
        return ultimaTareaEnviada;
    }

    public void setUltimaTareaEnviada(Tarea ultimaTareaEnviada) {
        this.ultimaTareaEnviada = ultimaTareaEnviada;
    }

    public List<Tarea> getUltimasTareas() {
        int limit = Integer.parseInt(settingHome.getValue("fede.dashboard.timeline.length", "5"));
        if (ultimasTareas.isEmpty()) {
//            ultimasTareas = tareaService.findByNamedQuery("Tarea.findLasts", limit, subject);
            ultimasTareas = tareaService.findByNamedQuery("Tarea.findLasts", subject);
        }
        return ultimasTareas;
    }

    public List<Tarea> getMisUltimasTareasEnviadas() {
        int limit = Integer.parseInt(settingHome.getValue("fede.dashboard.timeline.length", "5"));
        if (misUltimasTareasEnviadas.isEmpty()) {
            misUltimasTareasEnviadas = tareaService.findByNamedQuery("Tarea.findLastsByAuthor", subject);
        }
        return misUltimasTareasEnviadas;
    }

    public void setMisUltimasTareasEnviadas(List<Tarea> misUltimasTareasEnviadas) {
        this.misUltimasTareasEnviadas = misUltimasTareasEnviadas;
    }

    public List<Tarea> getMisUltimasTareasRecibidas() {
        int limit = Integer.parseInt(settingHome.getValue("fede.dashboard.timeline.length", "5"));
        if (misUltimasTareasRecibidas.isEmpty()) {
            misUltimasTareasRecibidas = tareaService.findByNamedQuery("Tarea.findLastsByOwner", subject, EstadoTipo.ESPERA);
        }
        return misUltimasTareasRecibidas;
    }

    public void setMisUltimasTareasRecibidas(List<Tarea> misUltimasTareasRecibidas) {
        this.misUltimasTareasRecibidas = misUltimasTareasRecibidas;
    }

    public Long getProcesoId() {
        return procesoId;
    }

    public void setProcesoId(Long procesoId) {
        this.procesoId = procesoId;
    }

    public Subject getSolicitante() {
        return solicitante;
    }

    public void setSolicitante(Subject solicitante) {
        this.solicitante = solicitante;
    }

    public void aceptarDocumento() {
        try {
            BeanUtils.copyProperties(documento, getDocumentoAceptar());
        } catch (IllegalAccessException | InvocationTargetException e) {
        }
    }

    /**
     * Guardar la tarea
     * TODO analisar conveniencia de migrar hacia la creación de instancia de proceso
     */
    public void save() {
        if (getDestinatario() == null || getSolicitante() == null){
            addErrorMessage(I18nUtil.getMessages("common.error"), I18nUtil.getMessages("error.task.persons"));
            return;
        }
        try {
            if (!tarea.isPersistent()) {//Comando nulo, es tarea nueva
                //Crear proceso y asignar a tarea
                this.proceso = procesoService.createInstance();
                this.proceso.setCode(SerialService.getGenerator().next()); //Crear un generador de Process ID
                this.proceso.setAuthor(subject);
                this.proceso.setProcesoTipo(ProcesoTipo.NEGOCIO);
                this.proceso.setOwner(getSolicitante()); //El solicitante del proceso o tramite

                procesoService.save(this.proceso.getId(), this.proceso);

                //Crear dos tareas iniciales para el nuevo proceso
                //1. Tarea recepción documentos, se realiza al momento de crear el proceso
                prepareTarea(getTarea(), settingHome.getValue("fede.documents.task.first.name", "Recepción de documentos"), getTarea().getDescription(), subject, subject, EstadoTipo.RESUELTO);
                tareaService.save(getTarea().getId(), getTarea());
                procesarDocumentos(getTarea());
                
                //2. Siguiente tarea
                Tarea _tarea = buildTarea(settingHome.getValue("fede.documents.task.second.name", "Evaluar documentación y redirigir"), "", subject, getDestinatario(), EstadoTipo.ESPERA);
                tareaService.save(_tarea.getId(), _tarea);
                
                //Enviar notificación de inicio de proceso
                sendNotification(this.proceso, "app.mail.template.process.start", false);
                //Enviar notificación de tarea por realizar
                sendNotification(_tarea, "app.mail.template.task.assign", false);
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
                sendNotification(getTarea(), "app.mail.template.task.assign", false);
            }
            this.addDefaultSuccessMessage();
        } catch (Exception e) {
            addErrorMessage(e, I18nUtil.getMessages("error.persistence"));
        }
    }
    
    
    public Tarea prepareTarea(Tarea _tarea, String name, String description, Subject author, Subject owner, EstadoTipo estado) {
        return prepareTarea(this.proceso, _tarea, name, description, author, owner, estado);
    }
    
    public Tarea prepareTarea(InstanciaProceso instanciaProceso, Tarea _tarea, String name, String description, Subject author, Subject owner, EstadoTipo estado) {
        //2. Siguiente tarea
        _tarea.setName(name);
        _tarea.setDescription(description);
        _tarea.setInstanciaProceso(instanciaProceso);
        //Es temporral hasta que se pueda seleccionar una organización
        _tarea.setDepartamento("temporal");
        _tarea.setAuthor(author); //usuario logeado
        _tarea.setOwner(owner); //destinatario
        _tarea.setEstadoTipo(estado);//La tarea se completa al iniciar el proceso
        return _tarea;
    }
    
    private Tarea buildTarea(String name, String description, Subject author, Subject owner, EstadoTipo estado) {
        //2. Siguiente tarea
        Tarea _tarea = tareaService.createInstance();
        _tarea.setName(name);
        _tarea.setDescription(description);
        _tarea.setInstanciaProceso(this.proceso);
        //Es temporral hasta que se pueda seleccionar una organización
        _tarea.setDepartamento("temporal");
        _tarea.setAuthor(author); //usuario logeado
        _tarea.setOwner(owner); //destinatario
        _tarea.setEstadoTipo(estado);//La tarea se completa al iniciar el proceso
        return _tarea;
    }
    
    public void sendNotification(InstanciaProceso instanciaProceso, String templateName, boolean displayMessage) {
        if (instanciaProceso.isPersistent()) {
            //Notificar alta en appsventas
            String url = settingHome.getValue("app.documents.url.process.detail", "http://localhost:8080/appsventas/pages/management/proceso/instancia_proceso.jsf?uuid=");
            String url_title = instanciaProceso.getName();
            Map<String, Object> values = new HashMap<>();
            
            //TODO implementar una forma de definición de parametros desde configuración
            values.put("instanciaProceso", instanciaProceso);
            values.put("url", url + instanciaProceso.getCode());
            values.put("url_title", url_title);

            if (templateHome.sendEmail(instanciaProceso.getAuthor(), settingHome.getValue(templateName, templateName), values)
                    && templateHome.sendEmail(instanciaProceso.getOwner(), settingHome.getValue(templateName, templateName), values)){
                if (displayMessage) addDefaultSuccessMessage();
            } else {
                if (displayMessage) addDefaultErrorMessage();
            }
        }
    }
    
    public void sendNotification(Tarea tarea, String templateName, boolean displayMessage) {
        if (tarea.isPersistent()) {
            //Notificar alta en appsventas
            String url = settingHome.getValue("app.documents.url.process.detail", "http://localhost:8080/appsventas/pages/management/proceso/instancia_prooceso.jsf?id=");
            String url_title = tarea.getName();
            Map<String, Object> values = new HashMap<>();
            
            //TODO implementar una forma de definición de parametros desde configuración
            values.put("tarea", tarea);
            values.put("url", url + tarea.getInstanciaProceso().getId());
            values.put("url_title", url_title);

            if (templateHome.sendEmail(tarea.getAuthor(), settingHome.getValue(templateName, templateName), values)
                    && templateHome.sendEmail(tarea.getOwner(), settingHome.getValue(templateName, templateName), values)){
                if (displayMessage) addDefaultSuccessMessage();
            } else {
                if (displayMessage) addDefaultErrorMessage();
            }
        }
    }
    
    public void sendNotification(Tarea tarea, String templateName, boolean displayMessage, boolean force) {
        if (!tarea.isPersistent() && force) {
            tarea.setId(-1L);//forzar el envio de la notificación
            sendNotification(tarea, templateName, displayMessage);
        } else {
            sendNotification(tarea, templateName, displayMessage);
        }
    }

    @Deprecated
    public void complete(Tarea t) {
        try {
            getSiguienteTarea().setInstanciaProceso(t.getInstanciaProceso());
            getSiguienteTarea().setName(t.getName());
            getSiguienteTarea().setAuthor(subject);
            getSiguienteTarea().setOwner(getDestinatario());
            getSiguienteTarea().setDepartamento("Temporal");
            getSiguienteTarea().setEstadoTipo(EstadoTipo.ESPERA);
            tareaService.save(getSiguienteTarea());
            procesarDocumentos(getSiguienteTarea());
            eliminarDocumentos();
            
            //Actualizar tarea en edición
            t.setEstadoTipo(EstadoTipo.RESUELTO);
            tareaService.save(t.getId(), t);
            this.addDefaultSuccessMessage();
        } catch (Exception e) {
            addErrorMessage(e, I18nUtil.getMessages("error.persistence"));
        }
    }

    public void eliminarDocumentos() {
        for (Documento doc : documentosRemovidos) {
            if (doc.isPersistent()) {
                documentoService.remove(doc.getId(), doc);
            }
            documentosRemovidos.remove(doc);
        }
    }

    public BigDecimal countRowsByTag(String tag) {
        BigDecimal total = new BigDecimal(0);
        if ("all".equalsIgnoreCase(tag)) {
            total = new BigDecimal(tareaService.count());
        } else if ("own".equalsIgnoreCase(tag)) {
            total = new BigDecimal(tareaService.count("Tarea.countBussinesEntityByOwner", subject));
        } else {
            total = new BigDecimal(tareaService.count("Tarea.countBussinesEntityByTagAndOwner", tag, subject));
        }
        return total;
    }

    public void setUltimasTareas(List<Tarea> ultimasTareas) {
        this.ultimasTareas = ultimasTareas;
    }

    @Override
    public void handleReturn(SelectEvent event) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public LazyTareaDataModel getLazyDataModel() {
        filter();
        return lazyDataModel;
    }

    public void setLazyDataModel(LazyTareaDataModel lazyDataModel) {
        this.lazyDataModel = lazyDataModel;
    }

    public void filter() {
        if (lazyDataModel == null) {
            lazyDataModel = new LazyTareaDataModel(tareaService);
        }

        lazyDataModel.setOwner(subject);
        lazyDataModel.setState(EstadoTipo.ESPERA);
        //lazyDataModel.setAuthor(subject);
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
     * Disparar la apertura de vista de instancia de proceso, para la tarea seleccionada
     * @param event evento click en tabla de datos
     */
    public void onRowSelect(SelectEvent event) {
        try {
            //Redireccionar a RIDE de objeto seleccionado
            if (event != null && event.getObject() != null) {
                //redirectTo("/pages/management/tarea/tarea.jsf?tareaId=" + ((BussinesEntity) event.getObject()).getId());
                Tarea _tarea = (Tarea) event.getObject();
                redirectTo("/pages/management/proceso/instancia_proceso.jsf?instanciaProcesoId=" + _tarea.getInstanciaProceso().getId());
            }
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(FacturaElectronicaHome.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void onRowUnselect(UnselectEvent event) {
        FacesMessage msg = new FacesMessage(I18nUtil.getMessages("BussinesEntity") + " " + I18nUtil.getMessages("common.unselected"), ((BussinesEntity) event.getObject()).getName());
        FacesContext.getCurrentInstance().addMessage(null, msg);
        logger.info(I18nUtil.getMessages("BussinesEntity") + " " + I18nUtil.getMessages("common.unselected"), ((BussinesEntity) event.getObject()).getName());
    }

    public void getDocumentos(Tarea t) {
        for (Documento doc : t.getDocumentos()) {
            doc.setContents(obtenerBytes(new File(doc.getRuta())));
        }
    }

    public byte[] obtenerBytes(File file) {
        ByteArrayOutputStream ous = null;
        @SuppressWarnings("UnusedAssignment")
        InputStream ios = null;
        try {
            byte[] buffer = new byte[4096];
            ous = new ByteArrayOutputStream();
            ios = new FileInputStream(file);
            @SuppressWarnings("UnusedAssignment")
            int read = 0;
            while ((read = ios.read(buffer)) != -1) {
                ous.write(buffer, 0, read);
            }
        } catch (FileNotFoundException ex) {

        } catch (IOException ex) {

        }
        return ous.toByteArray();
    }

    public Tarea getTarea() {
        if (tareaId != null && !this.tarea.isPersistent()) {
            this.tarea = tareaService.find(tareaId);
            getDocumentos(this.tarea);
            setDestinatario(tarea.getOwner());
            setSolicitante(tarea.getAuthor());
            setProceso(tarea.getInstanciaProceso());
        }
        return tarea;
    }

    public InstanciaProceso getProceso() {
        return proceso;
    }

    public void setProceso(InstanciaProceso proceso) {
        this.proceso = proceso;
    }

    public void setTarea(Tarea tarea) {
        this.tarea = tarea;
    }

    public Tarea getSiguienteTarea() {
        return siguienteTarea;
    }

    public void setSiguienteTarea(Tarea siguienteTarea) {
        this.siguienteTarea = siguienteTarea;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Long getTareaId() {
        return tareaId;
    }

    public void setTareaId(Long tareaId) {
        this.tareaId = tareaId;
    }

    public Subject getDestinatario() {
        return destinatario;
    }

    public void handleFileUpload(FileUploadEvent event) {
        procesarUploadFile(event.getFile());
    }

    public void handleFileUploadEdit(FileUploadEvent event) {
        if (documentoAceptar != null) {
            documentoAceptar.setContents(event.getFile().getContents());
            documentoAceptar.setMimeType(event.getFile().getContentType());
            documentoAceptar.setFileName(event.getFile().getFileName());
            documentoAceptar.setRuta(settingHome.getValue("app.management.tarea.documentos.ruta", "/tmp") + "//" + event.getFile().getFileName());
        }
    }

    public void procesarUploadFileSiguienteTarea(FileUploadEvent event) {
        if (event.getFile() == null) {
            this.addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("fede.file.null"));
            return;
        }

        if (subject == null) {
            this.addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("fede.subject.null"));
            return;
        }
        try {
            Documento doc = crearDocumento(event.getFile(), siguienteTarea);

            if (siguienteTarea != null) {
                siguienteTarea.getDocumentos().add(doc);
            }

            //Encerar el obeto para edición de nuevo documento
            setDocumento(documentoService.createInstance());

        } catch (Exception e) {
            this.addErrorMessage(I18nUtil.getMessages("action.fail"), e.getMessage());
        }
    }

    public void procesarUploadFile(UploadedFile file) {
        if (file == null) {
            this.addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("fede.file.null"));
            return;
        }

        if (subject == null) {
            this.addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("fede.subject.null"));
            return;
        }
        try {
            Documento doc = crearDocumento(file, tarea);
            if (tarea != null) {
                tarea.getDocumentos().add(doc);
            }
            //Encerar el obeto para edición de nuevo documento
            setDocumento(documentoService.createInstance());

        } catch (Exception e) {
            this.addErrorMessage(I18nUtil.getMessages("action.fail"), e.getMessage());
        }
    }

    /**
     * GRABAR DOCUMENTOS
     *
     * @param t
     */
    public void procesarDocumentos(Tarea t) {
        for (Documento doc : t.getDocumentos()) {
            generaDocumento(new File(doc.getRuta()), doc.getContents());
        }
    }

    private Documento crearDocumento(UploadedFile file, Tarea t) {
        Documento doc = documentoService.createInstance();
        doc.setTarea(t);
        doc.setOwner(t.getOwner());
        doc.setAuthor(t.getOwner());

        if (getDocumento() != null && Strings.isNullOrEmpty(getDocumento().getName())) {
            doc.setName(file.getFileName());
            doc.setDocumentType(DocumentType.UNDEFINED);
        } else {
            doc.setName(getDocumento().getName());
            doc.setDocumentType(getDocumento().getDocumentType());
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
        doc.setContents(file.getContents());
        return doc;
    }

    public void generaDocumento(File file, byte[] bytes) {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            try (BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                bos.write(bytes);
                bos.flush();
            }

        } catch (IOException ex) {
            addErrorMessage(ex, I18nUtil.getMessages("common.error.uploadfail"));
        }
    }

    public void setDestinatario(Subject destinatario) {
        this.destinatario = destinatario;
    }

    public Tarea getSelectedTarea() {
        return selectedTarea;
    }

    public void setSelectedTarea(Tarea selectedTarea) {
        this.selectedTarea = selectedTarea;
    }

    @Override
    public Group getDefaultGroup() {
        return null;
    }

    public Documento getDocumento() {
        return documento;
    }

    public void setDocumento(Documento documento) {
        this.documento = documento;
    }

    public void removerDocumento(Documento doc, Tarea t) {
        this.documentosRemovidos.add(doc);
        t.getDocumentos().remove(doc);
    }

    public void editarDocumento(Documento doc) {
        documentoAceptar = new Documento();
        setDocumento(doc);
        try {
            BeanUtils.copyProperties(documentoAceptar, doc);
            documentoAceptar.setId(doc.getId());
        } catch (IllegalAccessException | InvocationTargetException e) {
            System.out.println(e);
        }

        RequestContext context = RequestContext.getCurrentInstance();
        context.execute("PF('dlgDocumento').show();");
    }

    public Long getDocumentoId() {
        return documentoId;
    }

    public void setDocumentoId(Long documentoId) {
        this.documentoId = documentoId;
    }

    public OrganizationHome getOrganizationHome() {
        return organizationHome;
    }

    public void setOrganizationHome(OrganizationHome organizationHome) {
        this.organizationHome = organizationHome;
    }

    public List<Documento> getDocumentosRemovidos() {
        return documentosRemovidos;
    }

    public void setDocumentosRemovidos(List<Documento> documentosRemovidos) {
        this.documentosRemovidos = documentosRemovidos;
    }

    public StreamedContent downloadDocument(Documento doc) {
        StreamedContent fileDownload = null;
        try {
            if (doc != null) {
                InputStream stream = new FileInputStream(new File(doc.getRuta()));

                fileDownload = new DefaultStreamedContent(stream, doc.getMimeType(), doc.getFileName());
            }
        } catch (FileNotFoundException e) {
        }
        return fileDownload;
    }

    public Documento getDocumentoAceptar() {
        return documentoAceptar;
    }

    public void setDocumentoAceptar(Documento documentoAceptar) {
        this.documentoAceptar = documentoAceptar;
    }

   @Override
    public List<Group> getGroups() {
        if (groups.isEmpty()) {
            groups = groupService.findByOwnerAndModuleAndType(subject, "documents", Group.Type.LABEL);
        }

        return groups;
    }
}
