# 🏯 HeavenlyAuction (Vạn Bảo Lâu - Tu Tiên Các)

Plugin sàn giao dịch, chợ trời, huyền thưởng lệnh và đấu giá bảo vật chuyên sâu, an toàn dữ liệu tuyệt đối và tối ưu hóa hệ thống dành cho máy chủ Minecraft **Paper / Purpur / Spigot** (1.20.x – 1.21.x+).

---

## 🌟 Tính Năng Nổi Bật

### 1. 🛡️ Kiến Trúc Dữ Liệu Chuyên Nghiệp & Chống Tràn Bộ Nhớ (Anti-Overflow & Crash-Safe)
- **Cơ chế lưu trữ SQLite chuẩn công nghiệp**:
  - Toàn bộ dữ liệu nằm tách biệt trong thư mục con `plugins/HeavenlyAuction/data/database.db`.
  - Kích hoạt **WAL Mode (Write-Ahead Logging)**, `PRAGMA synchronous = NORMAL` và `busy_timeout = 5000` giúp truy vấn đọc/ghi bất đồng bộ (Async) thần tốc, không bao giờ gây giật lag Main Thread hay khóa database.
- **Tiến trình dọn dẹp định kỳ (Data Retention Engine)**:
  - Tự động thanh lọc các thư tín đã nhận và lịch sử giao dịch cũ hơn `retention-days` (mặc định 30 ngày).
  - Tự động thực thi `PRAGMA optimize` và lệnh nén `VACUUM` giải phóng triệt để dung lượng ổ đĩa, ngăn chặn việc dữ liệu phình to dẫn đến tràn bộ nhớ máy chủ.
- **Giới hạn số lượng nghiêm ngặt chống spam**:
  - Giới hạn tối đa bảo vật đăng sàn trên mỗi tu sĩ (`max-items-per-player`).
  - Giới hạn tối đa lệnh Huyền Thưởng cùng lúc (`max-bounties-per-player`).
  - Giới hạn sức chứa hòm thư Càn Khôn Đãi (`max-items-per-player`) chống kẻ gian lợi dụng làm sập server.
- **Khóa tương tranh phân tán (Concurrency Lock)**:
  - Cơ chế ReentrantLock chống triệt để lỗi Dupe đồ hoặc race condition khi 2 người chơi cùng lúc nhấp mua một món đồ hoặc cùng ra giá đấu.
  - Khi server tắt đột ngột hoặc dừng server, toàn bộ trạng thái phiên chợ, hòm thư và vật phẩm đều đã được cam kết an toàn trong SQLite (ACID compliant).

---

### 2. 💱 Tích Hợp Đa Tiền Tệ Tu Tiên (Multi-Economy Reflection Bridge)
Hỗ trợ cùng lúc 3 loại tiền tệ đặc trưng của máy chủ Tu Tiên:
1. **`eco`**: Bạc (Tích hợp thông qua **Vault**).
2. **`points`**: Linh Thạch (Tích hợp thông qua **PlayerPoints**).
3. **`xuhethong`**: Xu Hệ Thống (Tích hợp thông qua **CoinsEngine**).
- Toàn bộ hệ thống liên kết qua Reflection thông minh, hoạt động độc lập ngay cả khi không có thư viện phụ thuộc cứng lúc compile, chống `NoClassDefFoundError`.
- Hỗ trợ thiết lập **thuế sàn riêng biệt** cho từng loại tiền tệ (ví dụ: Bạc 10%, Linh Thạch 5%, Xu Hệ Thống 8%).

---

### 3. 🔮 Tương Thích Tuyệt Đối MMOItems, AceFormation, AceAlchemy
- Sử dụng thuật toán tuần tự hóa NBT bit-for-bit tiên tiến (`ItemSerializer`), bảo lưu nguyên vẹn 100%:
  - CustomModelData
  - Dữ liệu thẻ PersistentDataContainer (PDC)
  - Lore, Display Name đa tầng, mã màu Hex (`&#RRGGBB` / `<#RRGGBB>`)
  - Thuộc tính Custom Attributes, Flags, Enchantments ẩn của các plugin Tu Tiên (AceFormation, AceAlchemy, MMOItems, AceCultivation,...).
- So sánh chính xác vật phẩm giao nộp theo thẻ nhận diện đặc thù thay vì chỉ so sánh vật phẩm Minecraft thuần.

---

### 4. 📜 Huyền Thưởng Lệnh (Player-to-Player Bounty System)
- Cho phép người chơi tự lập lệnh treo thưởng để thu thập tài nguyên quý hiếm (mảnh quái thú, thảo dược luyện đan, khoáng thạch, thẻ trận pháp,...).
- Tiền thưởng được trích lập và bảo chứng an toàn vào hệ thống ngay khi tạo lệnh.
- Tu sĩ khác khi thu thập đủ số lượng có thể mở giao diện và bấm **Giao Nộp**. Hệ thống tự động kiểm tra túi đồ, thu hồi đúng số lượng và chuyển tiền thưởng cho người nộp, đồng thời gửi vật phẩm vào **Càn Khôn Đãi** của người phát lệnh.
- Hỗ trợ người tạo lệnh hủy treo thưởng để nhận lại 100% tiền cọc bất cứ lúc nào.

---

### 5. ⚡ Thiên Bảo Đấu Giá & Cổ Vật Ẩn Vị (Server Scheduled & Mystery Auction)
- **Đấu giá người chơi**: Tu sĩ tự mở phiên đấu giá cho bảo vật của mình với bước giá và thời gian linh hoạt.
- **Bảo vệ chống gian lận giá (Anti-Sniping)**: Nếu có người ra giá khi phiên đấu giá còn dưới 30 giây, thời gian tự động kéo dài thêm 30 giây để tạo sự công bằng.
- **Hoàn trả tiền cược tức thì**: Ngay khi có người trả giá cao hơn, toàn bộ số tiền cược của người trước đó sẽ được hoàn lại ngay vào tài khoản!
- **Đấu giá tự động của Hệ Thống (Server Scheduled)**:
  - Cứ mỗi **3 giờ** (180 phút), hệ thống tự động khởi động một phiên đấu giá ngẫu nhiên từ kho tàng báu vật.
  - **Sự Kiện Cổ Vật Ẩn Vị lúc 20:00 (8 giờ tối)**: Vào khung giờ vàng 20:00 hàng ngày, phiên đấu giá hệ thống sẽ phát động đấu giá **Cổ Vật Chưa Giám Định**. Vật phẩm được bao phủ bởi luồng khí hỗn mang thượng cổ huyền bí và các dòng mô tả cuốn hút; chỉ người chiến thắng mới phá vỡ phong ấn và nhận được bảo vật thật sự!

---

### 6. 🎒 Càn Khôn Đãi (Hòm Thư Tín & Lưu Trữ Cá Nhân)
- Mọi vật phẩm mua thành công khi túi đồ đầy, vật phẩm thu hoạch từ Huyền Thưởng Lệnh, tiền bán bảo vật, và bảo vật hết hạn niêm yết đều được chuyển về Càn Khôn Đãi (`/vbl kho`).
- Giao diện trực quan cho phép nhận từng món hoặc nhận nhanh toàn bộ tiền tệ và bảo vật chỉ với 1 click.

---

## 📋 Yêu Cầu Hệ Thống
- **Java**: Java 21 trở lên
- **Nền tảng**: Paper / Purpur / Folia / Spigot 1.20 - 1.21.x+
- **Kinh tế (Tùy chọn)**: Vault, PlayerPoints, CoinsEngine
- **Placeholder & Giao diện**: Hỗ trợ đầy đủ định dạng Hex và mã màu Tiên Hiệp.

---

## 🎮 Bảng Lệnh & Quyền Hạn

### Dành Cho Người Chơi (Quyền: `vbl.use` - Mặc định: Có)

| Lệnh | Mô tả |
| :--- | :--- |
| `/vbl` hoặc `/vbl menu` | Mở Tiên Các Vạn Bảo Lâu (Giao diện tổng đài trung tâm) |
| `/vbl market` hoặc `/vbl cho` | Mở trực tiếp Chợ Trời Vạn Bảo Lâu |
| `/vbl sell <giá> <loại_tiền>` | Đăng bán bảo vật đang cầm trên tay (`eco`, `points`, `xuhethong`) |
| `/vbl treo <số_lượng> <tiền_thưởng> <loại_tiền>` | Phát lệnh Huyền Thưởng thu thập bảo vật theo mẫu đang cầm |
| `/vbl daugia <giá_sàn> <bước_giá> <giây> <loại_tiền>` | Mở phiên đấu giá cho bảo vật đang cầm |
| `/vbl bid <id_phiên> <số_tiền>` | Tham gia trả giá trong phiên đấu giá bảo vật |
| `/vbl kho` hoặc `/vbl mailbox` | Mở Càn Khôn Đãi (Hòm thư nhận bảo vật và tiền tệ) |

*Mọi lệnh đều tích hợp Tab-Completion gợi ý thông minh đầy đủ các đối số.*

### Dành Cho Quản Trị Viên (Quyền: `vbl.admin` - Mặc định: OP)

| Lệnh | Mô tả |
| :--- | :--- |
| `/vbladmin reload` | Tải lại cấu hình `config.yml` và ngôn ngữ `lang.yml` |
| `/vbladmin startauction` | Kích hoạt ngay một phiên đấu giá hệ thống |
| `/vbladmin secretauction` | Kích hoạt ngay phiên đấu giá Cổ Vật Ẩn Vị (Mystery Item) |
| `/vbladmin purge` | Cưỡng chế dọn dẹp thư cũ, bản ghi lịch sử và nén `VACUUM` cơ sở dữ liệu SQLite |

---

## ⚙️ Cấu Hình Chi Tiết (`config.yml`)

```yaml
market:
  expire-time-seconds: 604800 # 7 ngày
  max-items-per-player: 10

currencies:
  eco:
    display-name: "&aBạc"
    tax-percent: 10.0
  points:
    display-name: "&bLinh Thạch"
    tax-percent: 5.0
  xuhethong:
    display-name: "&6Xu Hệ Thống"
    tax-percent: 8.0

bounties:
  expire-time-seconds: 604800
  max-bounties-per-player: 5

auctions:
  default-duration-seconds: 300
  min-bid-step: 100.0
  tax-percent: 5.0

server-auctions:
  enabled: true
  interval-minutes: 180 # 3 giờ
  duration-seconds: 300
  mystery-time: "20:00" # Khung giờ phát động Cổ Vật Ẩn Vị

mailbox:
  max-items-per-player: 50

data-retention:
  retention-days: 30
  cleanup-interval-minutes: 60
```
