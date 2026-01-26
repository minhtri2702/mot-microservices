package com.mot.repository;

import com.mot.entity.SocialAccounts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SocialAccountsRepository extends JpaRepository<SocialAccounts, Long> {

}
