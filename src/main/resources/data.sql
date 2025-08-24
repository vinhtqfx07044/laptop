-- =================================================================================================
-- SAMPLE DATA FOR LAPTOP REPAIR APPLICATION
-- =================================================================================================

-- -------------------------------------------------------------------------------------------------
-- Section 1: SERVICE ITEMS
-- -------------------------------------------------------------------------------------------------
INSERT INTO service_item (id, name, price, vat_rate, warranty_days, active, created_at, updated_at) VALUES
-- Nhân công chung (NC)
('550e8400-e29b-41d4-a716-446655440000', '[NC] Vệ sinh, bảo dưỡng toàn diện laptop', 250000, 0.10, 7, true, '2025-05-02 09:00:00', '2025-05-02 09:00:00'),
('550e8400-e29b-41d4-a716-446655440001', '[NC] Cài đặt lại hệ điều hành Windows 11 Pro', 300000, 0.10, 30, true, '2025-05-03 10:00:00', '2025-05-10 11:30:00'),
('550e8400-e29b-41d4-a716-446655440002', '[NC] Cài đặt MacOS Sonoma và các ứng dụng cơ bản', 400000, 0.10, 30, true, '2025-05-04 11:00:00', '2025-05-11 14:00:00'),
('550e8400-e29b-41d4-a716-446655440003', '[NC] Sửa chữa, hàn lại bản lề laptop', 350000, 0.10, 90, true, '2025-05-05 14:00:00', '2025-05-15 16:45:00'),
('550e8400-e29b-41d4-a716-446655440004', '[NC] Sửa lỗi mainboard không lên nguồn', 800000, 0.10, 90, true, '2025-05-06 15:30:00', '2025-05-20 09:15:00'),
('550e8400-e29b-41d4-a716-446655440005', '[NC] Khắc phục lỗi sạc không vào pin (phần cứng)', 450000, 0.10, 60, true, '2025-05-07 16:00:00', '2025-05-22 10:00:00'),
('550e8400-e29b-41d4-a716-446655440006', '[NC] Cứu dữ liệu ổ cứng bị bad sector', 600000, 0.10, 0, true, '2025-05-09 11:45:00', '2025-06-05 14:20:00'),
-- Nhân công thay thế (NC)
('550e8400-e29b-41d4-a716-446655440007', '[NC] Công thay thế/nâng cấp RAM, SSD', 150000, 0.10, 7, true, '2025-05-10 09:00:00', '2025-06-01 10:00:00'),
('550e8400-e29b-41d4-a716-446655440008', '[NC] Công thay thế màn hình', 300000, 0.10, 7, true, '2025-05-10 09:05:00', '2025-06-01 10:05:00'),
('550e8400-e29b-41d4-a716-446655440009', '[NC] Công thay thế bàn phím', 200000, 0.10, 7, true, '2025-05-10 09:10:00', '2025-06-01 10:10:00'),
('550e8400-e29b-41d4-a716-446655440010', '[NC] Công thay thế pin', 150000, 0.10, 7, true, '2025-05-10 09:15:00', '2025-06-01 10:15:00'),
('550e8400-e29b-41d4-a716-446655440011', '[NC] Công thay thế quạt tản nhiệt', 250000, 0.10, 7, true, '2025-05-10 09:20:00', '2025-06-01 10:20:00'),
-- Linh kiện (LK)
('550e8400-e29b-41d4-a716-446655440012', '[LK] RAM Kingston Fury Impact 16GB DDR5 4800MHz', 1850000, 0.08, 365, true, '2025-05-10 14:00:00', '2025-06-10 17:00:00'),
('550e8400-e29b-41d4-a716-446655440013', '[LK] Ổ cứng SSD Samsung 980 Pro 1TB PCIe NVMe Gen4', 2500000, 0.08, 365, true, '2025-05-11 15:00:00', '2025-06-12 09:30:00'),
('550e8400-e29b-41d4-a716-446655440014', '[LK] Màn hình 15.6" FHD IPS (1920x1080) 144Hz', 2800000, 0.08, 180, true, '2025-05-12 16:30:00', '2025-06-15 10:45:00'),
('550e8400-e29b-41d4-a716-446655440015', '[LK] Bàn phím HP Envy 13 (có đèn nền)', 750000, 0.08, 180, true, '2025-05-13 10:00:00', '2025-06-18 14:00:00'),
('550e8400-e29b-41d4-a716-446655440016', '[LK] Pin Laptop HP Spectre X360 (Mã HT03XL)', 950000, 0.08, 180, true, '2025-05-14 11:20:00', '2025-06-20 11:00:00'),
('550e8400-e29b-41d4-a716-446655440017', '[LK] Quạt tản nhiệt CPU cho Razer Blade 15', 650000, 0.08, 90, true, '2025-05-16 09:00:00', '2025-06-28 16:00:00');

-- -------------------------------------------------------------------------------------------------
-- Section 2: REQUESTS
-- -------------------------------------------------------------------------------------------------
INSERT INTO request (id, name, phone, email, address, brand_model, serial_number, appointment_date, description, status, completed_at) VALUES
('6ba7b810-9dad-11d1-80b4-00c04fd430c8', 'Lê Minh Tuấn', '0901123456', 'tuan.le@email.com', '10 Phan Xích Long, Phú Nhuận, TP.HCM', 'Dell XPS 15 9570', 'DL2023X15970A001', '2025-07-25 10:00:00', 'Máy tính rất nóng khi sử dụng và quạt kêu to bất thường. Cần kiểm tra hệ thống tản nhiệt.', 'SCHEDULED', NULL),
('6ba7b810-9dad-11d1-80b4-00c04fd430c9', 'Nguyễn Thị Hoa', '0912345678', 'hoa.nguyen@email.com', '25 Nguyễn Thị Minh Khai, Quận 1, TP.HCM', 'HP Envy 13', 'HP2024ENV13H002', '2025-07-26 14:00:00', 'Bàn phím bị liệt một vài phím (E, R, T). Không gõ được.', 'QUOTED', NULL),
('6ba7b810-9dad-11d1-80b4-00c04fd430ca', 'Trần Văn Bình', '0987654321', 'binh.tran@email.com', '300 Lê Văn Sỹ, Quận 3, TP.HCM', 'MacBook Pro 14" M1', 'MBP14M1PRO003', '2025-07-27 09:30:00', 'Màn hình bị sọc ngang, hiển thị sai màu. Đã thử khởi động lại nhưng không hết.', 'APPROVE_QUOTED', NULL),
('6ba7b810-9dad-11d1-80b4-00c04fd430cb', 'Phạm Thị Dung', '0939876543', 'dung.pham@email.com', '50A Trần Hưng Đạo, Quận 5, TP.HCM', 'Lenovo Yoga Slim 7', 'LN2023YGS7L004', '2025-07-20 11:00:00', 'Máy không sạc được pin, cắm sạc không có tín hiệu. Đã thử đổi ổ cắm.', 'IN_PROGRESS', NULL),
('6ba7b810-9dad-11d1-80b4-00c04fd430cc', 'Hoàng Văn Nam', '0945123789', 'nam.hoang@email.com', '123 Nguyễn Văn Cừ, Quận 5, TP.HCM', 'Asus TUF Gaming F15', 'AS2022TUFG15005', '2025-07-15 15:00:00', 'Máy chạy rất chậm, mở ứng dụng hay bị treo. Muốn nâng cấp thêm RAM và vệ sinh máy.', 'UNDER_WARRANTY', '2025-07-18 17:00:00'),
('6ba7b810-9dad-11d1-80b4-00c04fd430cd', 'Vũ Thị Lan', '0978456123', 'lan.vu@email.com', '88 Võ Văn Tần, Quận 3, TP.HCM', 'Acer Aspire 5', 'AC2023ASP5A006', '2025-07-10 16:00:00', 'Máy không kết nối được Wifi, đã thử cài lại driver nhưng không được.', 'CANCELLED', NULL),
('6ba7b810-9dad-11d1-80b4-00c04fd430ce', 'Đặng Minh Châu', '0905555888', 'chau.dang@email.com', '459 Sư Vạn Hạnh, Quận 10, TP.HCM', 'Microsoft Surface Laptop 4', 'MS2024SFL4C007', '2025-06-25 10:30:00', 'Ổ cứng đầy, máy khởi động và chạy ứng dụng rất ì ạch. Cần thay ổ SSD dung lượng lớn hơn.','COMPLETED', '2025-06-27 11:00:00'),
('6ba7b810-9dad-11d1-80b4-00c04fd430cf', 'Ngô Gia Huy', '0918888999', 'huy.ngo@email.com', '18E Cộng Hòa, Tân Bình, TP.HCM', 'Razer Blade 15', 'RZ2023BLD15G008', '2025-07-28 13:00:00', 'Chơi game một lúc là máy tự sập nguồn. Nhiệt độ CPU rất cao.',  'QUOTED', NULL),
('6ba7b810-9dad-11d1-80b4-00c04fd430d0', 'Lý Thu Thảo', '0966777888', 'thao.ly@email.com', '202 Tô Hiến Thành, Quận 10, TP.HCM', 'Dell Latitude 7420', 'DL2022LAT7420009', '2025-07-22 15:30:00', 'Làm rơi laptop, vỏ bị móp, bản lề gãy.', 'IN_PROGRESS', NULL),
('6ba7b810-9dad-11d1-80b4-00c04fd430d1', 'Bùi Anh Dũng', '0922333444', 'dung.bui@email.com', '79/15 Âu Cơ, Tân Bình, TP.HCM', 'HP Spectre X360', 'HP2023SPX360010', '2025-06-15 09:00:00', 'Pin dùng rất nhanh hết, chỉ được khoảng 1 tiếng. Cần thay pin mới.', 'UNDER_WARRANTY', '2025-06-15 11:30:00'),
('6ba7b810-9dad-11d1-80b4-00c04fd430d2', 'Đỗ Ngọc Anh', '0908111222', 'anh.do@email.com', '12bis Nguyễn Huệ, Quận 1, TP.HCM', 'MacBook Air M2', 'MBAM2AIR24011', '2025-07-29 16:30:00', 'Làm đổ nước vào máy, đã lau khô nhưng không dám bật nguồn. Cần kiểm tra gấp.', 'SCHEDULED', NULL),
('6ba7b810-9dad-11d1-80b4-00c04fd430d3', 'Mai Tiến Thành', '0913222333', 'thanh.mai@email.com', '56 Thành Thái, Quận 10, TP.HCM', 'LG Gram 17', 'LG2023GRM17012', '2025-07-24 17:00:00', 'Cần cài đặt một số phần mềm đồ họa chuyên dụng.', 'CANCELLED', NULL),
('6ba7b810-9dad-11d1-80b4-00c04fd430d4', 'Tô Hoài An', '0988999000', 'an.to@email.com', '99 Nguyễn Trãi, Quận 1, TP.HCM', 'Samsung Galaxy Book 3', 'SM2024GBK3013', '2025-07-28 18:00:00', 'Máy không nhận sạc, đã thử dây sạc khác vẫn không được.', 'APPROVE_QUOTED', NULL),
('6ba7b810-9dad-11d1-80b4-00c04fd430d5', 'Dương Hữu Nghĩa', '0903876543', 'nghia.duong@email.com', '332 Lý Thường Kiệt, Quận 11, TP.HCM', 'MSI Modern 14', 'MS2023MDN14014', '2025-07-01 14:00:00', 'Máy chạy chậm, muốn nâng cấp ổ cứng.', 'COMPLETED', '2025-07-02 16:00:00'),
('6ba7b810-9dad-11d1-80b4-00c04fd430d6', 'Châu Mỹ Lệ', '0919876543', 'le.chau@email.com', '100 Hùng Vương, Quận 5, TP.HCM', 'Asus Zenbook UX425', 'AS2024ZBK425015', '2025-07-23 11:30:00', 'Màn hình bị nhấp nháy liên tục, rất khó chịu.', 'IN_PROGRESS', NULL);

-- -------------------------------------------------------------------------------------------------
-- Section 3: REQUEST ITEMS
-- -------------------------------------------------------------------------------------------------
INSERT INTO request_items (id, request_id, service_item_id, name, price, vat_rate, quantity, discount, warranty_days) VALUES
-- Request 2: Báo giá thay bàn phím + công
('3f79d5d4-0b5e-4b3e-8b0e-5b6c7d8e9f0a', '6ba7b810-9dad-11d1-80b4-00c04fd430c9', '550e8400-e29b-41d4-a716-446655440015', '[LK] Bàn phím HP Envy 13 (có đèn nền)', 750000, 0.08, 1, 0, 180),
('3f79d5d4-0b5e-4b3e-8b0e-5b6c7d8e9f0b', '6ba7b810-9dad-11d1-80b4-00c04fd430c9', '550e8400-e29b-41d4-a716-446655440009', '[NC] Công thay thế bàn phím', 200000, 0.10, 1, 0, 7),
-- Request 3: Báo giá thay màn hình + công
('3f79d5d4-0b5e-4b3e-8b0e-5b6c7d8e9f0c', '6ba7b810-9dad-11d1-80b4-00c04fd430ca', '550e8400-e29b-41d4-a716-446655440014', '[LK] Màn hình 15.6" FHD IPS (1920x1080) 144Hz', 2800000, 0.08, 1, 0, 180),
('3f79d5d4-0b5e-4b3e-8b0e-5b6c7d8e9f0d', '6ba7b810-9dad-11d1-80b4-00c04fd430ca', '550e8400-e29b-41d4-a716-446655440008', '[NC] Công thay thế màn hình', 300000, 0.10, 1, 0, 7),
-- Request 4: Sửa lỗi sạc (Chỉ có NC)
('3f79d5d4-0b5e-4b3e-8b0e-5b6c7d8e9f0e', '6ba7b810-9dad-11d1-80b4-00c04fd430cb', '550e8400-e29b-41d4-a716-446655440005', '[NC] Khắc phục lỗi sạc không vào pin (phần cứng)', 450000, 0.10, 1, 0, 60),
-- Request 5: Nâng RAM + vệ sinh + công
('3f79d5d4-0b5e-4b3e-8b0e-5b6c7d8e9f0f', '6ba7b810-9dad-11d1-80b4-00c04fd430cc', '550e8400-e29b-41d4-a716-446655440000', '[NC] Vệ sinh, bảo dưỡng toàn diện laptop', 250000, 0.10, 1, 0, 7),
('3f79d5d4-0b5e-4b3e-8b0e-5b6c7d8e9f10', '6ba7b810-9dad-11d1-80b4-00c04fd430cc', '550e8400-e29b-41d4-a716-446655440012', '[LK] RAM Kingston Fury Impact 16GB DDR5 4800MHz', 1850000, 0.08, 1, 0, 365),
('3f79d5d4-0b5e-4b3e-8b0e-5b6c7d8e9f11', '6ba7b810-9dad-11d1-80b4-00c04fd430cc', '550e8400-e29b-41d4-a716-446655440007', '[NC] Công thay thế/nâng cấp RAM, SSD', 150000, 0.10, 1, 0, 7),
-- Request 7: Thay SSD + cài Win + công (công được miễn phí)
('3f79d5d4-0b5e-4b3e-8b0e-5b6c7d8e9f12', '6ba7b810-9dad-11d1-80b4-00c04fd430ce', '550e8400-e29b-41d4-a716-446655440013', '[LK] Ổ cứng SSD Samsung 980 Pro 1TB PCIe NVMe Gen4', 2500000, 0.08, 1, 0, 365),
('3f79d5d4-0b5e-4b3e-8b0e-5b6c7d8e9f13', '6ba7b810-9dad-11d1-80b4-00c04fd430ce', '550e8400-e29b-41d4-a716-446655440007', '[NC] Công thay thế/nâng cấp RAM, SSD', 150000, 0.10, 1, 150000, 7),
('3f79d5d4-0b5e-4b3e-8b0e-5b6c7d8e9f14', '6ba7b810-9dad-11d1-80b4-00c04fd430ce', '550e8400-e29b-41d4-a716-446655440001', '[NC] Cài đặt lại hệ điều hành Windows 11 Pro', 300000, 0.10, 1, 300000, 30),
-- Request 8: Vệ sinh + thay quạt + công
('3f79d5d4-0b5e-4b3e-8b0e-5b6c7d8e9f15', '6ba7b810-9dad-11d1-80b4-00c04fd430cf', '550e8400-e29b-41d4-a716-446655440000', '[NC] Vệ sinh, bảo dưỡng toàn diện laptop', 250000, 0.10, 1, 0, 7),
('3f79d5d4-0b5e-4b3e-8b0e-5b6c7d8e9f16', '6ba7b810-9dad-11d1-80b4-00c04fd430cf', '550e8400-e29b-41d4-a716-446655440017', '[LK] Quạt tản nhiệt CPU cho Razer Blade 15', 650000, 0.08, 1, 0, 90),
('3f79d5d4-0b5e-4b3e-8b0e-5b6c7d8e9f17', '6ba7b810-9dad-11d1-80b4-00c04fd430cf', '550e8400-e29b-41d4-a716-446655440011', '[NC] Công thay thế quạt tản nhiệt', 250000, 0.10, 1, 0, 7),
-- Request 9: Sửa bản lề (Chỉ có NC)
('3f79d5d4-0b5e-4b3e-8b0e-5b6c7d8e9f18', '6ba7b810-9dad-11d1-80b4-00c04fd430d0', '550e8400-e29b-41d4-a716-446655440003', '[NC] Sửa chữa, hàn lại bản lề laptop', 350000, 0.10, 1, 0, 90),
-- Request 10: Thay pin + công
('3f79d5d4-0b5e-4b3e-8b0e-5b6c7d8e9f19', '6ba7b810-9dad-11d1-80b4-00c04fd430d1', '550e8400-e29b-41d4-a716-446655440016', '[LK] Pin Laptop HP Spectre X360 (Mã HT03XL)', 950000, 0.08, 1, 0, 180),
('3f79d5d4-0b5e-4b3e-8b0e-5b6c7d8e9f1a', '6ba7b810-9dad-11d1-80b4-00c04fd430d1', '550e8400-e29b-41d4-a716-446655440010', '[NC] Công thay thế pin', 150000, 0.10, 1, 0, 7),
-- Request 13: Sửa lỗi sạc (Chỉ có NC)
('3f79d5d4-0b5e-4b3e-8b0e-5b6c7d8e9f1b', '6ba7b810-9dad-11d1-80b4-00c04fd430d4', '550e8400-e29b-41d4-a716-446655440005', '[NC] Khắc phục lỗi sạc không vào pin (phần cứng)', 450000, 0.10, 1, 0, 60),
-- Request 14: Nâng cấp ổ cứng + công
('3f79d5d4-0b5e-4b3e-8b0e-5b6c7d8e9f1c', '6ba7b810-9dad-11d1-80b4-00c04fd430d5', '550e8400-e29b-41d4-a716-446655440013', '[LK] Ổ cứng SSD Samsung 980 Pro 1TB PCIe NVMe Gen4', 2500000, 0.08, 1, 0, 365),
('3f79d5d4-0b5e-4b3e-8b0e-5b6c7d8e9f1d', '6ba7b810-9dad-11d1-80b4-00c04fd430d5', '550e8400-e29b-41d4-a716-446655440007', '[NC] Công thay thế/nâng cấp RAM, SSD', 150000, 0.10, 1, 0, 7),
-- Request 11: Kiểm tra máy bị đổ nước
('3f79d5d4-0b5e-4b3e-8b0e-5b6c7d8e9f1e', '6ba7b810-9dad-11d1-80b4-00c04fd430d2', '550e8400-e29b-41d4-a716-446655440004', '[NC] Sửa lỗi mainboard không lên nguồn', 800000, 0.10, 1, 0, 90);

-- -------------------------------------------------------------------------------------------------
-- Section 4: REQUEST HISTORY
-- -------------------------------------------------------------------------------------------------
INSERT INTO request_history (id, request_id, changes, created_at, created_by) VALUES
-- Request 1
('9b86d081-92dd-4bf3-8c3c-5d6e7f8e9f0a', '6ba7b810-9dad-11d1-80b4-00c04fd430c8', 'Tạo mới yêu cầu', '2025-07-24 10:00:00', 'Public'),
-- Request 2
('9b86d081-92dd-4bf3-8c3c-5d6e7f8e9f0b', '6ba7b810-9dad-11d1-80b4-00c04fd430c9', 'Tạo mới yêu cầu', '2025-07-25 14:00:00', 'Public'),
('9b86d081-92dd-4bf3-8c3c-5d6e7f8e9f0c', '6ba7b810-9dad-11d1-80b4-00c04fd430c9', 'Cập nhật hạng mục sửa chữa
Tổng tiền: 0 → 1,030,000 VND
Trạng thái: Đã lên lịch → Đã báo giá
Ghi chú: Đã báo giá thay bàn phím và công thay thế', '2025-07-26 15:00:00', 'staff'),
-- Request 3
('9b86d081-92dd-4bf3-8c3c-5d6e7f8e9f0d', '6ba7b810-9dad-11d1-80b4-00c04fd430ca', 'Tạo mới yêu cầu', '2025-07-26 09:30:00', 'Public'),
('9b86d081-92dd-4bf3-8c3c-5d6e7f8e9f0e', '6ba7b810-9dad-11d1-80b4-00c04fd430ca', 'Cập nhật hạng mục sửa chữa
Tổng tiền: 0 → 3,354,000 VND
Trạng thái: Đã lên lịch → Đã báo giá', '2025-07-27 10:30:00', 'staff'),
('9b86d081-92dd-4bf3-8c3c-5d6e7f8e9f0f', '6ba7b810-9dad-11d1-80b4-00c04fd430ca', 'Trạng thái: Đã báo giá → Đã duyệt báo giá
Ghi chú: Khách đã đồng ý thay màn hình.', '2025-07-27 11:00:00', 'staff'),
-- Request 5
('9b86d081-92dd-4bf3-8c3c-5d6e7f8e9f10', '6ba7b810-9dad-11d1-80b4-00c04fd430cc', 'Tạo mới yêu cầu', '2025-07-14 15:00:00', 'Public'),
('9b86d081-92dd-4bf3-8c3c-5d6e7f8e9f11', '6ba7b810-9dad-11d1-80b4-00c04fd430cc', 'Cập nhật hạng mục sửa chữa
Tổng tiền: 0 → 2,433,000 VND
Trạng thái: Đã lên lịch → Đã báo giá', '2025-07-15 16:00:00', 'staff'),
('9b86d081-92dd-4bf3-8c3c-5d6e7f8e9f12', '6ba7b810-9dad-11d1-80b4-00c04fd430cc', 'Trạng thái: Đã báo giá → Đã duyệt báo giá', '2025-07-16 10:00:00', 'staff'),
('9b86d081-92dd-4bf3-8c3c-5d6e7f8e9f13', '6ba7b810-9dad-11d1-80b4-00c04fd430cc', 'Trạng thái: Đã duyệt báo giá → Đang thực hiện', '2025-07-16 11:00:00', 'staff'),
('9b86d081-92dd-4bf3-8c3c-5d6e7f8e9f14', '6ba7b810-9dad-11d1-80b4-00c04fd430cc', 'Trạng thái: Đang thực hiện → Hoàn thành
Ghi chú: Đã nâng cấp RAM và vệ sinh xong.', '2025-07-18 17:00:00', 'staff'),
('9b86d081-92dd-4bf3-8c3c-5d6e7f8e9f15', '6ba7b810-9dad-11d1-80b4-00c04fd430cc', 'Trạng thái: Hoàn thành → Đang bảo hành
Ghi chú: Chuyển sang chế độ bảo hành theo quy định.', '2025-07-19 09:00:00', 'staff'),
-- Request 7
('9b86d081-92dd-4bf3-8c3c-5d6e7f8e9f1f', '6ba7b810-9dad-11d1-80b4-00c04fd430ce', 'Tạo mới yêu cầu', '2025-06-24 10:30:00', 'Public'),
('9b86d081-92dd-4bf3-8c3c-5d6e7f8e9f16', '6ba7b810-9dad-11d1-80b4-00c04fd430ce', 'Cập nhật hạng mục sửa chữa
Tổng tiền: 0 → 3,030,000 VND
Trạng thái: Đã lên lịch → Đã báo giá
Ghi chú: Báo giá thay SSD. Khuyến mãi miễn phí công thay và cài đặt.', '2025-06-25 11:30:00', 'staff'),
('9b86d081-92dd-4bf3-8c3c-5d6e7f8e9f17', '6ba7b810-9dad-11d1-80b4-00c04fd430ce', 'Trạng thái: Đã báo giá → Đã duyệt báo giá', '2025-06-25 14:00:00', 'staff'),
('9b86d081-92dd-4bf3-8c3c-5d6e7f8e9f18', '6ba7b810-9dad-11d1-80b4-00c04fd430ce', 'Trạng thái: Đã duyệt báo giá → Đang thực hiện', '2025-06-26 09:00:00', 'staff'),
('9b86d081-92dd-4bf3-8c3c-5d6e7f8e9f19', '6ba7b810-9dad-11d1-80b4-00c04fd430ce', 'Trạng thái: Đang thực hiện → Hoàn thành
Ghi chú: Đã thay SSD 1TB và cài lại Win. Miễn phí công thay và cài đặt.', '2025-06-27 11:00:00', 'staff'),
-- Request 10
('9b86d081-92dd-4bf3-8c3c-5d6e7f8e9f1a', '6ba7b810-9dad-11d1-80b4-00c04fd430d1', 'Tạo mới yêu cầu', '2025-06-14 09:00:00', 'Public'),
('9b86d081-92dd-4bf3-8c3c-5d6e7f8e9f1b', '6ba7b810-9dad-11d1-80b4-00c04fd430d1', 'Cập nhật hạng mục sửa chữa
Tổng tiền: 0 → 1,191,000 VND
Trạng thái: Đã lên lịch → Đã báo giá', '2025-06-15 10:00:00', 'staff'),
('9b86d081-92dd-4bf3-8c3c-5d6e7f8e9f1c', '6ba7b810-9dad-11d1-80b4-00c04fd430d1', 'Trạng thái: Đã báo giá → Đã duyệt báo giá', '2025-06-15 10:15:00', 'staff'),
('9b86d081-92dd-4bf3-8c3c-5d6e7f8e9f1d', '6ba7b810-9dad-11d1-80b4-00c04fd430d1', 'Trạng thái: Đã duyệt báo giá → Hoàn thành
Ghi chú: Đã thay pin mới.', '2025-06-15 11:30:00', 'staff'),
('9b86d081-92dd-4bf3-8c3c-5d6e7f8e9f1e', '6ba7b810-9dad-11d1-80b4-00c04fd430d1', 'Trạng thái: Hoàn thành → Đang bảo hành
Ghi chú: Pin đang trong thời gian bảo hành 180 ngày.', '2025-06-16 08:00:00', 'staff');