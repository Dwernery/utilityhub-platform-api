package com.utilityhub.api.db.repository.library;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.utilityhub.api.db.entity.library.ReadingGoal;

public interface ReadingGoalRepository extends JpaRepository<ReadingGoal, Integer> {

    List<ReadingGoal> findAllByOrderByYearAsc();
}
