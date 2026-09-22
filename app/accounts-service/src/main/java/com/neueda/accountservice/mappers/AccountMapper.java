package com.neueda.accountservice.mappers;

import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;
import com.neueda.accountservice.models.Account;
import com.neueda.accountservice.repositories.AccountRepository;

@Mapper
public interface AccountMapper extends AccountRepository {
    
    @Insert("INSERT INTO accounts (account_id, holder_name, cash_balance, status, version, created_at, last_updated, updated_by) " +
            "VALUES (#{accountId}, #{holderName}, #{cashBalance}, #{status}, #{version}, #{createdAt}, #{lastUpdated}, #{updatedBy})")
    @Override
    void save(Account account);
    
    @Select("SELECT account_id as accountId, holder_name as holderName, cash_balance as cashBalance, " +
            "status, version, created_at as createdAt, last_updated as lastUpdated, updated_by as updatedBy " +
            "FROM accounts WHERE account_id = #{accountId}")
    @Override
    Optional<Account> findById(String accountId);
    
    @Delete("DELETE FROM accounts WHERE account_id = #{accountId}")
    @Override
    void delete(String accountId);
    
    @Select("SELECT COUNT(*) > 0 FROM accounts WHERE account_id = #{accountId}")
    @Override
    boolean exists(String accountId);
    
    @Update("UPDATE accounts SET holder_name = #{holderName}, cash_balance = #{cashBalance}, " +
            "status = #{status}, version = #{version}, last_updated = #{lastUpdated}, updated_by = #{updatedBy} " +
            "WHERE account_id = #{accountId}")
    @Override
    void update(Account account);
}
