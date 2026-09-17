package com.swp391.g1.service;

import com.swp391.g1.entity.ExtracurricularActivity;

import java.util.List;
import java.util.Optional;

public interface ExtracurricularActivityService {

    ExtracurricularActivity create(ExtracurricularActivity activity);

    List<ExtracurricularActivity> findAll();

    Optional<ExtracurricularActivity> findById(Long id);

    ExtracurricularActivity update(Long id, ExtracurricularActivity activity);

    void deleteById(Long id);
}
