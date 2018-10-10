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
package org.jlgranda.fede.cache;

/**
 *
 * @author jlgranda
 */



import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.jlgranda.fede.ejb.SubjectService;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import org.jpapi.model.profile.Subject;

@Singleton
public class SubjectCacheProvider  implements Serializable {

    private static final long serialVersionUID = -5160962182428837094L;

    private LoadingCache<Long, Subject> subjectCache;
    
    @EJB
    SubjectService subjectService;

    public SubjectCacheProvider() {

        subjectCache = CacheBuilder.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build(
                        new CacheLoader<Long, Subject>() {
                            @Override
                            public Subject load(Long id) throws Exception {
                                return subjectService.find(id);
                            }
                        }
                );
    }

    public LoadingCache<Long, Subject> getSubjectCache() {
        return subjectCache;
    }
}