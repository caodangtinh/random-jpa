package com.github.kuros.random.jpa.database.mssql.provider;

import com.github.kuros.random.jpa.cache.Cache;
import com.github.kuros.random.jpa.metamodel.AttributeProvider;
import com.github.kuros.random.jpa.metamodel.model.EntityTableMapping;
import com.github.kuros.random.jpa.provider.ColumnCharacterLength;
import com.github.kuros.random.jpa.provider.SQLCharacterLengthProvider;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Copyright (c) 2015 Kumar Rohit
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, either version 3 of the License or any
 *    later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
public final class MSSQLCharacterLengthProvider implements SQLCharacterLengthProvider {

    private static final String QUERY = "SELECT isc.TABLE_NAME,\n" +
            "  COLUMN_NAME,\n" +
            "  CHARACTER_MAXIMUM_LENGTH\n" +
            "FROM INFORMATION_SCHEMA.COLUMNS isc\n" +
            "  INNER JOIN information_schema.tables ist\n" +
            "    ON isc.table_name = ist.table_name\n" +
            "WHERE Table_Type = 'BASE TABLE' and CHARACTER_MAXIMUM_LENGTH > 0";

    private Map<String, ColumnCharacterLength> columnLengthsByTable;

    private final EntityManager entityManager;
    private static MSSQLCharacterLengthProvider instance;

    private MSSQLCharacterLengthProvider() {
        this.entityManager = Cache.getInstance().getEntityManager();
        this.columnLengthsByTable = init();
    }

    public static MSSQLCharacterLengthProvider getInstance() {
        if (instance == null) {
            instance = new MSSQLCharacterLengthProvider();
        }
        return instance;
    }

    private Map<String, ColumnCharacterLength> init() {

        final AttributeProvider attributeProvider = AttributeProvider.getInstance();

        final Map<String, ColumnCharacterLength> lengths = new HashMap<String, ColumnCharacterLength>();
        final Query query = entityManager.createNativeQuery(QUERY);
        final List resultList = query.getResultList();
        for (Object o : resultList) {
            final Object[] row = (Object[]) o;

            final EntityTableMapping entityTableMapping = attributeProvider.get((String) row[0]);

            if (entityTableMapping == null) {
                continue;
            }

            final String attributeName = entityTableMapping.getAttributeName((String) row[1]);
            final Integer length = (Integer) row[2];

            final String entityName = entityTableMapping.getEntityName();
            ColumnCharacterLength columnCharacterLength = lengths.get(entityName);
            if (columnCharacterLength == null) {
                columnCharacterLength = ColumnCharacterLength.newInstance();
                lengths.put(entityName, columnCharacterLength);
            }

            columnCharacterLength.add(attributeName, length);
        }

        return lengths;
    }

    public Integer getMaxLength(final String entityName, final String attributeName) {
        final ColumnCharacterLength columnCharacterLength = columnLengthsByTable.get(entityName);
        return columnCharacterLength != null ? columnCharacterLength.getLength(attributeName) : null;
    }

}
