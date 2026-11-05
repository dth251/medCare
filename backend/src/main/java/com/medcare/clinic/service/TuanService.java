package com.medcare.clinic.service;

import com.medcare.clinic.dto.tuan.TuanDTO;
import com.medcare.clinic.repository.TuanLamViecRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TuanService {

    private final TuanLamViecRepository tuanLamViecRepository;

    /** Danh sách tuần làm việc, mới nhất trước. */
    @Transactional(readOnly = true)
    public List<TuanDTO> danhSach() {
        return tuanLamViecRepository.findAllByOrderByNgayBatDauDesc().stream()
                .map(HoSoMapper::tuan)
                .toList();
    }
}
