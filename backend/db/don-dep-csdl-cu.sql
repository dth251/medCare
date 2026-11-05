-- =========================================================================
-- DỌN CSDL CŨ CHO KHỚP VỚI ENTITY HIỆN TẠI (chạy 1 lần trên CSDL đã có dữ liệu)
--
-- CSDL phongkham_m2 được Hibernate (ddl-auto: update) tạo dần qua nhiều phiên bản
-- code, nên còn sót bảng/cột của thiết kế cũ mà code không còn dùng.
-- Script này KHÔNG xóa dữ liệu nào:
--   * 3 bảng của thiết kế cũ được CHUYỂN sang schema "legacy" (vẫn truy vấn được).
--   * Chỉ xóa các cột đã kiểm tra là rỗng hoàn toàn (mọi dòng đều NULL).
--   * Bỏ ràng buộc NOT NULL sai trên cột cũ của tbl_minh_chung_khieu_nai
--     (nếu không, mọi lệnh lưu minh chứng mới sẽ lỗi).
--
-- Chạy:  psql -U postgres -d phongkham_m2 -f don-dep-csdl-cu.sql
-- Nên sao lưu trước:  pg_dump -U postgres -Fc -f truoc-don-dep.dump phongkham_m2
-- =========================================================================
BEGIN;

CREATE SCHEMA IF NOT EXISTS legacy;

-- 1. Bảng của thiết kế cũ (đơn giá tiền công / đơn khiếu nại / biên bản điều chỉnh).
--    Code hiện tại gộp khiếu nại + biên bản vào tbl_yeu_cau_khieu_nai và lưu đơn giá
--    trực tiếp ở tbl_nhan_suyte.don_gia_tien_cong.
ALTER TABLE IF EXISTS public.tbl_don_gia_tien_cong   SET SCHEMA legacy;
ALTER TABLE IF EXISTS public.tbl_don_khieu_nai       SET SCHEMA legacy;
ALTER TABLE IF EXISTS public.tbl_bien_ban_dieu_chinh SET SCHEMA legacy;

-- 2. Cột phân công từng được gộp vào tbl_bang_phan_cong (nay nằm ở tbl_phan_cong_ca).
--    Chỉ xóa khi toàn bộ giá trị là NULL.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = 'public' AND table_name = 'tbl_bang_phan_cong' AND column_name = 'nhan_su_id') THEN
        IF EXISTS (SELECT 1 FROM public.tbl_bang_phan_cong
                   WHERE nhan_su_id IS NOT NULL OR ca_truc_khoa_id IS NOT NULL OR vai_tro IS NOT NULL OR ghi_chu IS NOT NULL) THEN
            RAISE EXCEPTION 'tbl_bang_phan_cong còn dữ liệu ở cột cũ – kiểm tra thủ công trước khi xóa';
        END IF;
        ALTER TABLE public.tbl_bang_phan_cong
            DROP COLUMN IF EXISTS nhan_su_id,
            DROP COLUMN IF EXISTS ca_truc_khoa_id,
            DROP COLUMN IF EXISTS vai_tro,
            DROP COLUMN IF EXISTS ghi_chu,
            DROP COLUMN IF EXISTS dieu_dong_ngoai_dang_ky;
    END IF;
END $$;

-- 3. Cột liên kết tới bảng cũ: giữ lại (có thể chứa dữ liệu) nhưng không được bắt buộc.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = 'public' AND table_name = 'tbl_minh_chung_khieu_nai' AND column_name = 'don_khieu_nai_id') THEN
        ALTER TABLE public.tbl_minh_chung_khieu_nai ALTER COLUMN don_khieu_nai_id DROP NOT NULL;
    END IF;
END $$;

COMMIT;
