/*
 * Copyright (C) 2019 jlgranda
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
package org.jlgranda.fede.ui.util;

import com.jlgranda.fede.ejb.SubjectService;
import java.io.IOException;
import javax.ejb.EJB;
import org.jpapi.model.profile.Subject;
import org.omnifaces.cdi.GraphicImageBean;
import org.omnifaces.util.Faces;
import org.omnifaces.util.Utils;

/**
 *
 * @author jlgranda
 */
@GraphicImageBean
public class Images {
   

    @EJB
    SubjectService subjectService;
    public byte[] getContent(Long id) throws IOException {
        // Note: this is a dummy example. In reality, you should be able to return the desired byte[] content from some
        // service class by given ID.
//        return Utils.toByteArray(photo));
        Subject s = subjectService.find(id);
        if (s.getPhoto() != null && s.getPhoto().length > 0)
            return s.getPhoto();
        else
            return  Utils.toByteArray(Faces.getResourceAsStream("/resources/appsventas/images/default-profile.jpg"));
    }


}
