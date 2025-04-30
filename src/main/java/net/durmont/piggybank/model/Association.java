package net.durmont.piggybank.model;

import io.quarkus.logging.Log;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import org.apache.commons.beanutils.BeanUtils;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Entity
public class Association extends ConvertedEntity {

    public String regex;
    @ManyToOne(targetEntity = Account.class)
    public Account account;
    @ManyToOne(targetEntity = Instance.class)
    public Instance instance;
    public String mongoId;

    public Association() {
    }

    public Association(String json) {
        super(json);
    }

    public static Uni<List<Association>> list(Long instanceId, Association filter, Sort sort, Page page) {
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
        return Association.find(query, sort, params)
          .list();
    }

    public static Uni<Long> delete(Long instanceId, Long id) {
        return Association.delete("instance.id=:instance_id and id=:id", Parameters.with("instance_id",instanceId).and("id",id));
    }

    @Override
    public int hashCode() {
        int result = regex != null ? regex.hashCode() : 0;
        result = 31 * result + (account != null ? account.hashCode() : 0);
        result = 31 * result + (mongoId != null ? mongoId.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Association)) return false;

        Association that = (Association) o;

        if (!Objects.equals(regex, that.regex)) return false;
        if (!Objects.equals(account, that.account)) return false;
        return Objects.equals(mongoId, that.mongoId);
    }

    @Override
    public void setDefaults() {

    }

    public Uni<Association> update() {
        return Association.<Association>findById(id)
          .map(associationDb -> {
              if (associationDb != null && associationDb.instance != null && !Objects.equals(instance.id, associationDb.instance.id))
                  return null;
              try {
                  BeanUtils.copyProperties(associationDb, this);
              } catch (IllegalAccessException | InvocationTargetException e) {
                  Log.error("Error copying new properties of Association "+id, e);
                  throw new RuntimeException(e);
              }
              return associationDb;
          });
    }

}
