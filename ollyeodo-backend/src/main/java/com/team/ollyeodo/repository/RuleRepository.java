package com.team.ollyeodo.repository;

import com.team.ollyeodo.domain.Rule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RuleRepository extends JpaRepository<Rule, String> {
}