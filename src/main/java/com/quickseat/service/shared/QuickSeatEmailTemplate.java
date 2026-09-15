package com.quickseat.service.shared;

import java.math.BigDecimal;

public final class QuickSeatEmailTemplate {
    private QuickSeatEmailTemplate() {
    }

    public static String verification(String name, String verificationLink) {
        String safeName = escape(name);
        String safeLink = escape(verificationLink);
        return layout("VERIFY YOUR EMAIL", "Welcome to QuickSeat, " + safeName,
                "Confirm your email address to reserve seats and manage your bookings.", """
                <p style="margin:0 0 24px;color:#a7a8b6;font:16px/24px Arial,Helvetica,sans-serif;">Your verification link is valid for <strong style="color:#f5f5f7;">24 hours</strong>.</p>
                <table role="presentation" cellspacing="0" cellpadding="0" border="0" style="margin:0 auto;"><tr><td style="border-radius:8px;background:#ff3f63;"><a href="%s" style="display:inline-block;padding:14px 28px;color:#ffffff;font:700 16px Arial,Helvetica,sans-serif;text-decoration:none;">Verify email</a></td></tr></table>
                <p style="margin:24px 0 0;color:#737480;font:13px/20px Arial,Helvetica,sans-serif;word-break:break-all;">If the button does not work, copy this link into your browser:<br><a href="%s" style="color:#ff6b85;text-decoration:underline;">%s</a></p>
                """.formatted(safeLink, safeLink, safeLink));
    }

    public static String passwordResetOtp(String name, String otp) {
        return layout("PASSWORD RESET", "Reset your QuickSeat password",
                "Use the one-time code below to continue. Never share this code with anyone.", """
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="margin:0 0 24px;border:1px solid #3a3a40;border-radius:10px;background:#27272b;"><tr><td align="center" style="padding:22px 16px;color:#ff5a77;font:700 32px/36px Arial,Helvetica,sans-serif;letter-spacing:9px;">%s</td></tr></table>
                <p style="margin:0;color:#a7a8b6;font:16px/24px Arial,Helvetica,sans-serif;">This code expires in <strong style="color:#f5f5f7;">10 minutes</strong>. If you did not request a password reset, you can safely ignore this email.</p>
                """.formatted(escape(otp)));
    }

    public static String bookingConfirmation(String bookingReference, String movie, String cinema, String screen,
                                             String showtime, String seats, BigDecimal totalAmount) {
        String details = detailRow("Booking reference", bookingReference)
                + detailRow("Movie", movie)
                + detailRow("Cinema", cinema)
                + detailRow("Screen", screen)
                + detailRow("Showtime", showtime)
                + detailRow("Seats", seats)
                + detailRow("Total", totalAmount.toPlainString() + " MMK");
        return layout("BOOKING CONFIRMED", "Your seats are reserved",
                "Your booking is confirmed. Present the attached QR ticket at your cinema.", """
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="margin:0 0 22px;border:1px solid #3a3a40;border-radius:10px;background:#202024;">%s</table>
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="border-left:3px solid #ff3f63;background:#29171d;"><tr><td style="padding:15px 16px;color:#f5f5f7;font:14px/21px Arial,Helvetica,sans-serif;"><strong>QR ticket attached</strong><br><span style="color:#b9bac4;">Keep this email or download your ticket before arriving at the cinema.</span></td></tr></table>
                """.formatted(details));
    }

    private static String layout(String eyebrow, String title, String introduction, String content) {
        return """
                <!doctype html>
                <html lang="en"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"></head>
                <body style="margin:0;padding:0;background:#09090b;color:#f5f5f7;">
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" bgcolor="#09090b"><tr><td style="padding:36px 16px;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" align="center" style="max-width:600px;margin:0 auto;border:1px solid #303036;border-radius:16px;background:#18181c;">
                    <tr><td align="center" style="padding:34px 32px 22px;border-bottom:1px solid #303036;">
                      <div style="color:#ff4265;font:700 30px Arial,Helvetica,sans-serif;letter-spacing:-1.2px;">QuickSeat</div>
                      <div style="margin-top:10px;color:#ff7890;font:700 11px Arial,Helvetica,sans-serif;letter-spacing:2px;">%s</div>
                    </td></tr>
                    <tr><td style="padding:32px;">
                      <h1 style="margin:0 0 12px;color:#f7f7f8;font:700 25px/32px Arial,Helvetica,sans-serif;">%s</h1>
                      <p style="margin:0 0 26px;color:#b9bac4;font:16px/24px Arial,Helvetica,sans-serif;">%s</p>
                      %s
                    </td></tr>
                    <tr><td style="padding:20px 32px;border-top:1px solid #303036;color:#737480;font:12px/18px Arial,Helvetica,sans-serif;text-align:center;">QuickSeat &middot; More than movies. A better you.</td></tr>
                  </table>
                </td></tr></table>
                </body></html>
                """.formatted(escape(eyebrow), title, escape(introduction), content);
    }

    private static String detailRow(String label, String value) {
        return """
                <tr><td style="padding:11px 16px 4px;color:#8c8d98;font:600 12px Arial,Helvetica,sans-serif;text-transform:uppercase;letter-spacing:.7px;">%s</td></tr>
                <tr><td style="padding:0 16px 12px;color:#f5f5f7;font:16px/22px Arial,Helvetica,sans-serif;">%s</td></tr>
                """.formatted(escape(label), escape(value));
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
