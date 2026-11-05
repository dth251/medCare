-- =========================================================================
-- HỆ THỐNG QUẢN LÝ PHÒNG KHÁM TƯ MEDCARE – LƯỢC ĐỒ CSDL (PostgreSQL)
--
-- File này được SINH TỰ ĐỘNG từ các entity JPA (Hibernate 6, Spring Boot 3.2)
-- nên luôn khớp với code. Tên bảng/cột theo quy ước của Hibernate:
-- camelCase -> snake_case (vd. tenDangNhap -> ten_dang_nhap).
--
-- Dùng để:
--   * Tạo CSDL mới rỗng:  createdb phongkham_m2 && psql -d phongkham_m2 -f schema.sql
--   * Làm tài liệu lược đồ cho báo cáo.
-- Ứng dụng KHÔNG tự chạy file này (spring.sql.init.mode=never); khi khởi động,
-- Hibernate (ddl-auto: update) tự bổ sung bảng/cột còn thiếu.
--
-- Cách sinh lại sau khi sửa entity: xem README.md, mục "Sinh lại schema.sql".
--
-- Thiết kế CSDL ban đầu (có bảng tblDonGiaTienCong, tblDonKhieuNai,
-- tblBienBanDieuChinh riêng) được lưu tại thiet-ke-ban-dau/schema-thiet-ke.sql.
-- Khác biệt chính so với thiết kế ban đầu:
--   * Đơn giá ca lưu trực tiếp ở tbl_nhan_suyte.don_gia_tien_cong.
--   * Đơn khiếu nại + biên bản điều chỉnh gộp thành tbl_yeu_cau_khieu_nai.
-- =========================================================================

    create table tbl_bac_si (
        id bigint not null,
        hoc_vi varchar(30) not null check (hoc_vi in ('TIEN_SI','THAC_SI','CHUYEN_KHOA_I','CHUYEN_KHOA_II','DAI_HOC','CU_NHAN','CAO_DANG')),
        ma_bac_si varchar(30) not null unique,
        primary key (id)
    );

    create table tbl_bang_phan_cong (
        id bigserial not null,
        ma_bang varchar(50) not null,
        ngay_lap timestamp(6) not null,
        ngay_cong_bo timestamp(6),
        trang_thai varchar(30) not null check (trang_thai in ('BAN_NHAP','DANG_XEP','DA_DU','DA_CONG_BO')),
        khoa_id bigint not null,
        tuan_lam_viec_id bigint not null,
        nguoi_lap_id bigint,
        ca_truc_khoa_id bigint,
        nhan_su_id bigint,
        vai_tro varchar(30) check (vai_tro in ('BAC_SI_CHINH','Y_TA_1','Y_TA_2')),
        ghi_chu TEXT,
        dieu_dong_ngoai_dang_ky boolean default false not null,
        primary key (id)
    );

    create table tbl_ca_lam_viec (
        gio_bat_dau time(6) not null,
        gio_ket_thuc time(6) not null,
        he_so_luong numeric(4,2) not null,
        id bigserial not null,
        loai_ca varchar(20) not null check (loai_ca in ('CHINH','NGOAI_GIO')),
        ten_ca varchar(50) not null,
        mo_ta TEXT,
        primary key (id)
    );

    create table tbl_ca_truc_khoa (
        dang_mo_cua boolean not null,
        ca_lam_viec_id bigint not null,
        id bigserial not null,
        khoa_id bigint not null,
        ngay_trong_tuan_id bigint not null,
        mo_ta TEXT,
        primary key (id),
        unique (khoa_id, ngay_trong_tuan_id, ca_lam_viec_id)
    );

    create table tbl_cham_cong (
        bang_phan_cong_id bigint not null,
        gio_check_in timestamp(6),
        gio_check_out timestamp(6),
        id bigserial not null,
        phieu_thanh_toan_id bigint,
        trang_thai varchar(30) not null check (trang_thai in ('DUNG_GIO','DI_MUON','VE_SOM','VANG_MAT','LOI_THIET_BI')),
        ghi_chu TEXT,
        primary key (id)
    );

    create table tbl_dang_ky_ca (
        ca_truc_khoa_id bigint not null,
        id bigserial not null,
        ngay_dang_ky timestamp(6) not null,
        phieu_dang_ky_id bigint not null,
        trang_thai varchar(20) not null check (trang_thai in ('HOP_LE','CHO_XEP','DA_XEP','TU_CHOI')),
        primary key (id),
        unique (phieu_dang_ky_id, ca_truc_khoa_id)
    );

    create table tbl_dia_chi (
        id bigserial not null,
        so_nha varchar(50),
        phuong_xa varchar(100),
        quan_huyen varchar(100),
        tinh_thanh varchar(100),
        toa_nha varchar(100),
        xom_pho varchar(150),
        primary key (id)
    );

    create table tbl_khoa (
        id bigserial not null,
        phong_kham_id bigint not null,
        ma_khoa varchar(20) not null,
        ten varchar(100) not null,
        mo_ta TEXT,
        primary key (id),
        unique (phong_kham_id, ma_khoa)
    );

    create table tbl_minh_chung_khieu_nai (
        id bigserial not null,
        yeu_cau_khieu_nai_id bigint,
        duong_dan varchar(255) not null,
        primary key (id)
    );

    create table tbl_nam_lam_viec (
        id bigserial not null,
        ten varchar(50) not null unique,
        mo_ta TEXT,
        primary key (id)
    );

    create table tbl_ngay_trong_tuan (
        ngay date not null,
        id bigserial not null,
        tuan_lam_viec_id bigint not null,
        thu varchar(20) not null check (thu in ('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY')),
        mo_ta TEXT,
        primary key (id),
        unique (ngay)
    );

    create table tbl_nhan_suyte (
        don_gia_tien_cong numeric(18,2),
        id bigint not null,
        khoa_id bigint not null,
        primary key (id)
    );

    create table tbl_nhan_vien (
        id bigint not null,
        vi_tri_cong_viec varchar(30) not null check (vi_tri_cong_viec in ('QUAN_LY','KE_TOAN')),
        primary key (id)
    );


    create table tbl_phieu_dang_ky (
        tong_so_ca integer not null,
        id bigserial not null,
        ngay_dang_ky timestamp(6) not null,
        nhan_su_id bigint not null,
        tuan_lam_viec_id bigint not null,
        trang_thai varchar(20) not null check (trang_thai in ('DA_LUU','DA_KHOA','HUY')),
        ma_phieu varchar(50) not null,
        primary key (id),
        unique (ma_phieu),
        unique (nhan_su_id, tuan_lam_viec_id)
    );

    create table tbl_phieu_thanh_toan (
        don_gia_ca_ap_dung numeric(12,2) not null,
        he_so_ngoai_gio_ap_dung numeric(4,2) not null,
        phat numeric(12,2) not null,
        phu_cap_thu_thuat numeric(12,2) not null,
        phu_cap_trach_nhiem numeric(12,2) not null,
        so_ca_chinh_goc integer not null,
        so_ca_ngoai_gio_goc integer not null,
        thuong numeric(12,2) not null,
        tong_thuc_linh numeric(12,2) not null,
        id bigserial not null,
        ke_toan_id bigint not null,
        ngay_lap timestamp(6) not null,
        ngay_thanh_toan timestamp(6),
        nhan_su_id bigint not null,
        tuan_lam_viec_id bigint not null,
        yeu_cau_khieu_nai_id bigint unique,
        hinh_thuc_thanh_toan varchar(30) not null check (hinh_thuc_thanh_toan in ('TRUC_TUYEN','TRUC_TIEP')),
        trang_thai varchar(30) not null check (trang_thai in ('CHUA_THANH_TOAN','DA_THANH_TOAN')),
        ma_phieu varchar(50) not null,
        so_tai_khoan_nhan varchar(50),
        chu_tai_khoan_nhan varchar(100),
        ma_giao_dich varchar(100),
        ngan_hang_nhan varchar(100),
        minh_chung_nhan_tien varchar(255),
        primary key (id),
        unique (ma_phieu),
        unique (nhan_su_id, tuan_lam_viec_id)
    );

    create table tbl_phong_kham (
        dia_chi_id bigint not null,
        id bigserial not null,
        so_dien_thoai varchar(20),
        ten varchar(150) not null,
        mo_ta TEXT,
        primary key (id)
    );

    create table tbl_thanh_vien (
        ngay_sinh date,
        dia_chi_id bigint,
        id bigserial not null,
        so_dien_thoai varchar(20),
        ho varchar(50),
        so_tai_khoan varchar(50),
        ten varchar(50),
        ten_dang_nhap varchar(50) not null unique,
        ten_dem varchar(50),
        chu_tai_khoan varchar(100),
        email varchar(100),
        ten_ngan_hang varchar(100),
        mat_khau varchar(255) not null,
        primary key (id)
    );

    create table tbl_tuan_lam_viec (
        ngay_bat_dau date not null,
        ngay_ket_thuc date not null,
        so_ca_toi_da integer not null,
        so_ca_toi_da_moi_ngay integer not null,
        so_ca_toi_thieu integer not null,
        han_dang_ky timestamp(6),
        id bigserial not null,
        nam_lam_viec_id bigint not null,
        thoi_diem_mo_dang_ky timestamp(6),
        trang_thai varchar(30) not null check (trang_thai in ('DANG_MO_DANG_KY','DA_DONG_DANG_KY','DANG_XEP_LICH','DA_CONG_BO','KET_THUC')),
        ten varchar(50) not null,
        mo_ta TEXT,
        primary key (id),
        unique (nam_lam_viec_id, ten),
        unique (ngay_bat_dau)
    );

    create table tbl_yeu_cau_khieu_nai (
        don_gia_ca_ap_dung numeric(18,2),
        he_so_ngoai_gio_ap_dung numeric(5,2),
        khau_tru numeric(18,2),
        phu_cap_bo_sung numeric(18,2),
        so_ca_chinh_dieu_chinh integer not null,
        so_ca_ngoai_gio_dieu_chinh integer not null,
        so_tien_dieu_chinh numeric(18,2),
        bang_phan_cong_id bigint,
        cham_cong_id bigint,
        id bigserial not null,
        ngay_gui timestamp(6) not null,
        ngay_xu_ly timestamp(6),
        nguoi_xu_ly_id bigint,
        nhan_su_id bigint,
        phieu_thanh_toan_id bigint,
        trang_thai varchar(50) not null,
        ds_minh_chung TEXT,
        ly_do TEXT,
        ly_do_phan_hoi TEXT,
        ma_yeu_cau varchar(255) not null,
        noi_dung TEXT,
        primary key (id)
    );

    create table tblyta (
        id bigint not null,
        mayta varchar(30) not null unique,
        trinh_do varchar(30) not null check (trinh_do in ('TIEN_SI','THAC_SI','CHUYEN_KHOA_I','CHUYEN_KHOA_II','DAI_HOC','CU_NHAN','CAO_DANG')),
        primary key (id)
    );

    alter table if exists tbl_bac_si 
       add constraint FKcfa0lricb9qggiepxs13oygnh 
       foreign key (id) 
       references tbl_nhan_suyte;

    alter table if exists tbl_bang_phan_cong 
       add constraint FKcoui2gincr0qi2s9b2aye0hmd 
       foreign key (khoa_id) 
       references tbl_khoa;

    alter table if exists tbl_bang_phan_cong 
       add constraint FK27l16ny08dnq4pnh95ksk0kop 
       foreign key (nguoi_lap_id) 
       references tbl_nhan_vien;

    alter table if exists tbl_bang_phan_cong 
       add constraint FK8hkmg3u2kewj1vpe4cpj4m6kv 
       foreign key (tuan_lam_viec_id) 
       references tbl_tuan_lam_viec;

    alter table if exists tbl_ca_truc_khoa 
       add constraint FKaxyq647jwklqv9kmqfabrlggc 
       foreign key (ca_lam_viec_id) 
       references tbl_ca_lam_viec;

    alter table if exists tbl_ca_truc_khoa 
       add constraint FKchw185clr29xe8783htc9wn4i 
       foreign key (khoa_id) 
       references tbl_khoa;

    alter table if exists tbl_ca_truc_khoa 
       add constraint FKja29udrxyyhb22kxit1xau5ma 
       foreign key (ngay_trong_tuan_id) 
       references tbl_ngay_trong_tuan;

    alter table if exists tbl_cham_cong 
       add constraint FKlesvjgc6n9o4ensv8s9bvf84q 
       foreign key (bang_phan_cong_id) 
       references tbl_bang_phan_cong;

    alter table if exists tbl_cham_cong 
       add constraint FKbr9wctauff6qaobqcc3xy9lmo 
       foreign key (phieu_thanh_toan_id) 
       references tbl_phieu_thanh_toan;

    alter table if exists tbl_dang_ky_ca 
       add constraint FKpk26rusk09lo01i14xmke9vxv 
       foreign key (ca_truc_khoa_id) 
       references tbl_ca_truc_khoa;

    alter table if exists tbl_dang_ky_ca 
       add constraint FKh4mc8g7cepvlqls0gl6o2sgx1 
       foreign key (phieu_dang_ky_id) 
       references tbl_phieu_dang_ky;

    alter table if exists tbl_khoa 
       add constraint FKd2b25f7m7w5j9mekd9tk6o9hr 
       foreign key (phong_kham_id) 
       references tbl_phong_kham;

    alter table if exists tbl_minh_chung_khieu_nai 
       add constraint FKbv6a599eefxujwf3tmphpwfh9 
       foreign key (yeu_cau_khieu_nai_id) 
       references tbl_yeu_cau_khieu_nai;

    alter table if exists tbl_ngay_trong_tuan 
       add constraint FKfoxol6ot98bnujb05hrt1x9bb 
       foreign key (tuan_lam_viec_id) 
       references tbl_tuan_lam_viec;

    alter table if exists tbl_nhan_suyte 
       add constraint FK66dxukruwrhbfjjn1wrhj9nl6 
       foreign key (khoa_id) 
       references tbl_khoa;

    alter table if exists tbl_nhan_suyte 
       add constraint FKfoy4gbkwfyujejk9pwa3xs55r 
       foreign key (id) 
       references tbl_thanh_vien;

    alter table if exists tbl_nhan_vien 
       add constraint FKkwxysm4ydbso3ybes62epfifu 
       foreign key (id) 
       references tbl_thanh_vien;

    alter table if exists tbl_phieu_dang_ky 
       add constraint FKmlsmclw8hp35k5pw4p2yywjxu 
       foreign key (nhan_su_id) 
       references tbl_nhan_suyte;

    alter table if exists tbl_phieu_dang_ky 
       add constraint FKesphh0c800era1dxvti8bfxov 
       foreign key (tuan_lam_viec_id) 
       references tbl_tuan_lam_viec;

    alter table if exists tbl_phieu_thanh_toan 
       add constraint FKetl2i4j0uwbpmhy0xlw5sqwt5 
       foreign key (ke_toan_id) 
       references tbl_nhan_vien;

    alter table if exists tbl_phieu_thanh_toan 
       add constraint FKhif2yfs1la2ij9gbxeayqkcbq 
       foreign key (nhan_su_id) 
       references tbl_nhan_suyte;

    alter table if exists tbl_phieu_thanh_toan 
       add constraint FKbqkg0qg4fre5cvmpteii8s56f 
       foreign key (tuan_lam_viec_id) 
       references tbl_tuan_lam_viec;

    alter table if exists tbl_phieu_thanh_toan 
       add constraint FKput3n31aw4ev9qq98e1k36hc2 
       foreign key (yeu_cau_khieu_nai_id) 
       references tbl_yeu_cau_khieu_nai;

    alter table if exists tbl_phong_kham 
       add constraint FK7ffrie0w3re0c100plc787otj 
       foreign key (dia_chi_id) 
       references tbl_dia_chi;

    alter table if exists tbl_thanh_vien 
       add constraint FKhvc8dp3yw8ygksj9f4msoogl1 
       foreign key (dia_chi_id) 
       references tbl_dia_chi;

    alter table if exists tbl_tuan_lam_viec 
       add constraint FKne2snlbfalu7m4ywg7eiffkp6 
       foreign key (nam_lam_viec_id) 
       references tbl_nam_lam_viec;

    alter table if exists tbl_yeu_cau_khieu_nai 
       add constraint FKs5m75wp1pid6uyvhmher2dgbj 
       foreign key (bang_phan_cong_id) 
       references tbl_bang_phan_cong;

    alter table if exists tbl_yeu_cau_khieu_nai 
       add constraint FKgg5oahihn1m5n1nlrqm1xq3mu 
       foreign key (cham_cong_id) 
       references tbl_cham_cong;

    alter table if exists tbl_yeu_cau_khieu_nai 
       add constraint FKb3ubdn9i90cju3ufpnsalcfom 
       foreign key (nguoi_xu_ly_id) 
       references tbl_nhan_vien;

    alter table if exists tbl_yeu_cau_khieu_nai 
       add constraint FKo8ulwqntkm0sdutgywgs2lkx3 
       foreign key (nhan_su_id) 
       references tbl_nhan_suyte;

    alter table if exists tbl_yeu_cau_khieu_nai 
       add constraint FKp28au57n8rpiyf3dc6l8mlme0 
       foreign key (phieu_thanh_toan_id) 
       references tbl_phieu_thanh_toan;

    alter table if exists tblyta 
       add constraint FK2d79g613a1uumv7c9tgscpjrq 
       foreign key (id) 
       references tbl_nhan_suyte;
