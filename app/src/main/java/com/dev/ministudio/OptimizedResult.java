package com.dev.ministudio;

/**
 * 🪄 คลาสโมเดลเก็บข้อมูลผลลัพธ์การจัดแจงและปรับปรุงประสิทธิภาพซอร์สโค้ดจาก AI
 * แยกแยะระหว่างตัวโค้ดใหม่ (ไม่มีตัวหนังสือปะปน) และคำอธิบายเชิงลึกภาษาไทย
 */
public class OptimizedResult {
    public String updatedCode;   // ซอร์สโค้ดฉบับปรับปรุงใหม่ทั้งหมดแบบสมบูรณ์
    public String explanation;   // คำอธิบายภาษาไทยระบุรายละเอียดจุดแก้ไข

    public OptimizedResult(String updatedCode, String explanation) {
        this.updatedCode = updatedCode;
        this.explanation = explanation;
    }
}
