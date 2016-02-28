/*
 * Copyright (C) 2016 Jorge
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
package org.jlgranda.fede.servlet;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.tecnopro.document.ejb.DocumentoService;
import net.tecnopro.document.model.Documento;

/**
 *
 * @author Jorge
 */
@WebServlet(name = "fedeServlet", urlPatterns = {"/fedeServlet/*"})
public class FedeServlet extends HttpServlet {

    @Inject
    private DocumentoService documentoService;

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            String entity = request.getParameter("entity");
            String entityId = request.getParameter("id");
            if (entity == null) {
                return;
            }
            switch (entity) {
                case "documento":
                    Documento documento = documentoService.find(Long.parseLong(entityId));
                    if (documento == null) {
                        response.sendError(HttpServletResponse.SC_NOT_FOUND); // 404.
                        return;
                    }
                    File file = new File(documento.getRuta());
                    byte[] contents = getContent(file);
                    documento.setContents(contents != null ? contents : null);
                    if (documento.getContents() == null) {
                        response.sendError(HttpServletResponse.SC_NOT_FOUND); // 404.
                        return;
                    }
                    response.setCharacterEncoding("ISO-8859-1");
                    response.setContentType("application/pdf");
                    response.setContentLength(documento.getContents().length);
                    response.getOutputStream().write(documento.getContents());
                    response.getOutputStream().flush();
                    response.getOutputStream().close();
                    break;
            }
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    public byte[] getContent(File file) {
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
}
