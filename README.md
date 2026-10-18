#  Kriptoloji: Güvenli Haberleşme Projesi (Sunucu - İstemci)

Bu proje, **Kriptoloji** dersi kapsamında geliştirilmektedir. Proje, iki istemcinin birbirine doğrudan bağlanmadan, merkezi bir sunucu üzerinden güvenli (AES-128) ve güvensiz modda mesaj/PDF transferi yapmasını sağlar.

##  Seçilen Platform ve Teknoloji
- **Programlama Dili:** Java (Saf TCP Soket Mimarisi)
- **Arayüz:** Java Swing / JavaFX (Masaüstü Uygulaması)
- **Ağ Analizi:** Wireshark

*Not: Ekipteki herkesin ortak geliştirme ortamına sahip olması ve Wireshark paket analizlerinin en temiz şekilde (TCP seviyesinde) yakalanabilmesi amacıyla Java tercih edilmiştir.*

##  Grup Üyeleri ve Görev Dağılımı

1. **FATİH MUHAMMED ÇELİK - 445852:** 
   - Proje yöneticisi, GitHub branch yönetimi.
   - Java ile Sunucu (Server) ve İstemci (Client) soket haberleşme iskeletinin kurulması.
   - PDF dosyalarının byte dizilerine çevrilip ağ üzerinden aktarılması.

2. **BAHATTİN ADIYAMAN - 445855:**
   - Kütüphanesiz (Manuel) AES-128 motorunun (S-Box, ShiftRows, MixColumns vb.) sıfırdan yazılması.
   - Kütüphaneli AES ile manuel AES arasındaki farkların test edilip rapora dökülmesi.

3. **NESTAN HAMZA ÇETİN - 445858:**
   - Güvenli mod için anahtar dağıtım yöntemlerinin (RSA-2048, DH-2048, ECC, Kyber ML-KEM-768) projeye entegrasyonu.
   - Masaüstü arayüzünün (Mod seçimi, mesajlaşma ekranı) tasarlanması.
   - Sistem tamamlandığında ölçümlerin (milisaniye ve byte) yapılıp Wireshark .pcapng kayıtlarının alınması.


