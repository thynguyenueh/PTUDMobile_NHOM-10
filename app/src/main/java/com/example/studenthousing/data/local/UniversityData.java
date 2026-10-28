package com.example.studenthousing.data.local;

import com.example.studenthousing.data.model.University;

import java.util.ArrayList;
import java.util.List;

public class UniversityData {

    private static final List<University> UNIVERSITIES = new ArrayList<>();
    static {
        UNIVERSITIES.add(new University("CSND_1", "ĐH Cảnh sát Nhân dân", "Cơ sở 1", 10.733667, 106.699543, true));
        UNIVERSITIES.add(new University("CSND_2", "ĐH Cảnh sát Nhân dân", "Cơ sở 2", 10.858780, 106.755288, true));
        UNIVERSITIES.add(new University("VLU", "ĐH Văn Lang", "Cơ sở chính", 10.762849, 106.596931, false));
        UNIVERSITIES.add(new University("FPT", "ĐH FPT", "Cơ sở TP.HCM", 10.852560, 106.629268, true));
        UNIVERSITIES.add(new University("GTVT_2", "ĐH Giao thông Vận tải", "Cơ sở 2", 10.846869, 106.794616, true));
        UNIVERSITIES.add(new University("UTH", "ĐH Giao thông Vận tải TP.HCM", "Cơ sở chính", 10.804742, 106.716648, true));
        UNIVERSITIES.add(new University("HSU", "ĐH Hoa Sen", "Cơ sở chính", 10.770408, 106.692513, true));
        UNIVERSITIES.add(new University("HVU", "ĐH Hùng Vương", "Cơ sở chính", 10.753095, 106.660281, true));
        UNIVERSITIES.add(new University("HCMUS_1", "ĐH Khoa học Tự nhiên - ĐHQG", "Cơ sở 1 (Q.5)", 10.762717, 106.682343, true));
        UNIVERSITIES.add(new University("HCMUS_2", "ĐH Khoa học Tự nhiên - ĐHQG", "Cơ sở 2 (Thủ Đức)", 10.875322, 106.797505, true));
        UNIVERSITIES.add(new University("USSH_1", "ĐH KHXH & NV - ĐHQG", "Cơ sở 1 (Q.1)", 10.786037, 106.702743, true));
        UNIVERSITIES.add(new University("USSH_2", "ĐH KHXH & NV - ĐHQG", "Cơ sở 2 (Thủ Đức)", 10.872048, 106.801756, true));
        UNIVERSITIES.add(new University("UEL", "ĐH Kinh tế - Luật - ĐHQG", "Cơ sở Thủ Đức", 10.869769, 106.778070, true));
        UNIVERSITIES.add(new University("UEF", "ĐH Kinh tế - Tài chính TP.HCM", "Cơ sở Điện Biên Phủ", 10.799195, 106.706566, false));
        UNIVERSITIES.add(new University("UEH", "ĐH Kinh tế TP.HCM (UEH)", "Cơ sở Nguyễn Đình Chiểu", 10.783219, 106.694750, true));
        UNIVERSITIES.add(new University("UARCH", "ĐH Kiến trúc TP.HCM", "Cơ sở chính", 10.782218, 106.694335, true));
        UNIVERSITIES.add(new University("ULSA_2", "ĐH Lao động - Xã hội", "Cơ sở 2", 10.866170, 106.616747, true));
        UNIVERSITIES.add(new University("ULAW", "ĐH Luật TP.HCM", "Cơ sở Nguyễn Tất Thành", 10.767545, 106.705578, false));
        UNIVERSITIES.add(new University("NLU", "ĐH Nông Lâm TP.HCM", "Cơ sở Thủ Đức", 10.871220, 106.791490, true));
        UNIVERSITIES.add(new University("NTTU_Q12", "ĐH Nguyễn Tất Thành", "Cơ sở Q.12", 10.859730, 106.694660, true));
        UNIVERSITIES.add(new University("SGU", "ĐH Sài Gòn", "Cơ sở An Dương Vương", 10.760000, 106.682000, false));
        UNIVERSITIES.add(new University("TDTU", "ĐH Tôn Đức Thắng", "Cơ sở Tân Phong", 10.731800, 106.698080, true));
        UNIVERSITIES.add(new University("HCMUT", "ĐH Bách Khoa TP.HCM", "Cơ sở Lý Thường Kiệt", 10.777135, 106.660052, true));
    }

    public static List<University> getUniversities() {
        return UNIVERSITIES;
    }

    public static University findByCode(String code) {
        if (code == null) return null;
        for (University u : UNIVERSITIES) {
            if (u.getCode().equalsIgnoreCase(code)) {
                return u;
            }
        }
        return null;
    }
}
