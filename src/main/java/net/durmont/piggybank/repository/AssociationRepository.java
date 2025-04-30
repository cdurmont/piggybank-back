package net.durmont.piggybank.repository;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.logging.Log;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import net.durmont.piggybank.model.Association;
import net.durmont.piggybank.model.Instance;
import org.apache.commons.beanutils.BeanUtils;

import jakarta.enterprise.context.ApplicationScoped;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@ApplicationScoped
public class AssociationRepository implements PanacheRepository<Association> {

    public Uni<Association> create(Long instanceId, Association newAssociation) {
        if (newAssociation.instance == null)
            newAssociation.instance = new Instance();
        newAssociation.instance.id = instanceId;

        return Panache.withTransaction( () -> persist(newAssociation));
    }

    public Uni<List<Association>> list(Long instanceId, Association filter, Sort sort, Page page) {

        Map<String, Object> params = new HashMap<>();
        String query ="1=1";
        query+=" AND instance.id=:instance_id";
        params.put("instance_id", instanceId);
        if (filter != null) {
            if (filter.id!= null) {
                query+=" AND id=:id";
                params.put("id", filter.id);
            }
            if (filter.regex!= null) {
                query+=" AND regex LIKE :regex";
                params.put("regex", "%"+filter.regex+"%");
            }
            if (filter.account!= null && filter.account.id != null) {
                query+=" AND account.id=:account_id";
                params.put("account_id", filter.account.id);
            }
        }
        return find(query, sort, params)
                .list();
    }

    public Uni<Association> update(Long instanceId, Long id, Association association) {
        association.id = id;
        return Panache.withTransaction(
                () -> findById(id)
                        .map(associationDb -> {
                            if (associationDb != null && associationDb.instance != null && !Objects.equals(instanceId, associationDb.instance.id))
                                return null;
                            try {
                                BeanUtils.copyProperties(associationDb, association);
                            } catch (IllegalAccessException | InvocationTargetException e) {
                                Log.error("Error copying new properties of Association "+id, e);
                                throw new RuntimeException(e);
                            }
                            return associationDb;
                        })
        );
    }

    public Uni<Long> delete(Long instanceId, Long id) {
        return Panache.withTransaction(
                () -> delete("instance.id=:instance_id and id=:id", Parameters.with("instance_id",instanceId).and("id",id))
        );
    }
}
