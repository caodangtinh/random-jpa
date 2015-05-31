package com.github.kuros.random.jpa.database.mssql.provider;

import com.github.kuros.random.jpa.provider.ForeignKeyRelation;
import com.github.kuros.random.jpa.provider.RelationshipProvider;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Kumar Rohit on 4/22/15.
 */
public final class MSSQLRelationshipProvider implements RelationshipProvider {

    private static final String QUERY = "SELECT\n" +
            "  tp.name 'parent_table',\n" +
            "  cp.name 'parent_attribute',\n" +
            "  tr.name 'referenced_table',\n" +
            "  cr.name 'referenced_attribute'--,\n" +
            "FROM\n" +
            "  sys.foreign_keys fk\n" +
            "  INNER JOIN\n" +
            "  sys.tables tp ON fk.parent_object_id = tp.object_id\n" +
            "  INNER JOIN\n" +
            "  sys.tables tr ON fk.referenced_object_id = tr.object_id\n" +
            "  INNER JOIN\n" +
            "  sys.foreign_key_columns fkc ON fkc.constraint_object_id = fk.object_id\n" +
            "  INNER JOIN\n" +
            "  sys.columns cp ON fkc.parent_column_id = cp.column_id AND fkc.parent_object_id = cp.object_id\n" +
            "  INNER JOIN\n" +
            "  sys.columns cr ON fkc.referenced_column_id = cr.column_id AND fkc.referenced_object_id = cr.object_id\n" +
            "  ORDER BY\n" +
            "  tp.name, cp.column_id";

    private EntityManager entityManager;

    MSSQLRelationshipProvider(final EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public static MSSQLRelationshipProvider newInstance(final EntityManager entityManager) {
        return new MSSQLRelationshipProvider(entityManager);
    }

    @Override
    public List<ForeignKeyRelation> getForeignKeyRelations() {
        final List<ForeignKeyRelation> foreignKeyRelations = new ArrayList<ForeignKeyRelation>();

        final Query query = entityManager.createNativeQuery(QUERY);
        final List resultList = query.getResultList();
        for (Object o : resultList) {
            final Object[] row = (Object[]) o;

            final ForeignKeyRelation relation = ForeignKeyRelation.newInstance((String)row[0], (String)row[1], (String) row[2], (String)row[3]);
            foreignKeyRelations.add(relation);
        }

        return foreignKeyRelations;
    }

}