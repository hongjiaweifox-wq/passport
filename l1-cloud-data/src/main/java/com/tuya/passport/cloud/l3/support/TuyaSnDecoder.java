package com.tuya.passport.cloud.l3.support;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TuyaSnDecoder {
 private static final Pattern PATTERN=Pattern.compile("^C([A-F])([A-NP-R])([A-G])([A-G])([PECRGYD])(\\d{2})(\\d{2})(\\d{2})(\\d{4})$");
 private static final Map<String,String> PRODUCTS=Map.ofEntries(Map.entry("A","CBE2000"),Map.entry("B","CBE2000S"),Map.entry("C","CBE2000 Pro"),Map.entry("D","CBE2000 AC"),Map.entry("E","CBE Expansion Battery 2000"),Map.entry("F","CBE Expansion Battery 5000"),Map.entry("G","Lyra2500 AC"),Map.entry("H","Lyra2500 Pro"),Map.entry("I","CBE5000 Pro"),Map.entry("J","Atlas6000 AC"),Map.entry("K","Nova Air EC SP"),Map.entry("L","Nova Air Battery 5000"),Map.entry("M","Nova Apex EC SP"),Map.entry("N","Nova Apex EC TP"),Map.entry("P","Nova Apex Battery 6000"),Map.entry("Q","Nova Apex Battery 8000"),Map.entry("R","PowerSync"));
 private static final Map<String,String> FACTORIES=Map.of("A","天宝","B","豪鹏","C","鹏元晟","D","鹏威","E","九联","F","海鹏科技","G","未知工厂");
 private static final Map<String,String> BRANDS=Map.of("A","Conow","B","cabay","C","华宇","D","代勒兰德","E","中性","F","易阳","G","Altilia");
 private static final Map<String,String> CELLS=Map.of("P","鹏辉","E","亿纬锂能","C","楚能","R","瑞浦兰钧","G","国轩高科","Y","融捷","D","多氟多");
 private static final Map<String,String> SCHEMES=Map.of("A","底部加热膜，无气溶胶","B","侧面加热膜，无气溶胶","C","底部加热膜，有气溶胶","D","侧面加热膜，有气溶胶","E","无加热，无气溶胶","F","无加热，有气溶胶");
 private TuyaSnDecoder(){}
 public static Optional<DecodedSn> decode(String raw){if(raw==null)return Optional.empty();String normalized=java.text.Normalizer.normalize(raw.trim(),java.text.Normalizer.Form.NFC).toUpperCase(Locale.ROOT);Matcher m=PATTERN.matcher(normalized);if(!m.matches())return Optional.empty();try{int yy=Integer.parseInt(m.group(6));LocalDate date=LocalDate.of(2000+yy,Integer.parseInt(m.group(7)),Integer.parseInt(m.group(8)));String productCode=m.group(2);return Optional.of(new DecodedSn(normalized,m.group(1),SCHEMES.get(m.group(1)),productCode,PRODUCTS.get(productCode),m.group(3),FACTORIES.get(m.group(3)),m.group(4),BRANDS.get(m.group(4)),m.group(5),CELLS.get(m.group(5)),date,m.group(9),"SN-"+productCode+m.group(3)+"-"+date.format(DateTimeFormatter.BASIC_ISO_DATE)));}catch(Exception ex){return Optional.empty();}}
 public static SnRule rule(){
  return new SnRule("V7.0",16,PATTERN.pattern(),"CAACBE2508040001",
   List.of(
    new Segment("X1","固定码","1 位","固定字符 C，代表 CONOW"),
    new Segment("X2","选配方案","1 位，A–F","见选配方案代码表"),
    new Segment("X3","产品机型","1 位，A–R（不含 O）","见产品机型代码表"),
    new Segment("X4","生产工厂","1 位，A–G","见生产工厂代码表"),
    new Segment("X5","品牌","1 位，A–G","见品牌代码表"),
    new Segment("X6","电芯品牌","1 位","见电芯品牌代码表"),
    new Segment("X7-X8","制造年份","2 位数字","年份后两位，例如 2024 → 24"),
    new Segment("X9-X10","制造月份","2 位数字","01–12"),
    new Segment("X11-X12","制造日期","2 位数字","01–31"),
    new Segment("X13-X16","流水号","4 位数字","由系统自动生成")),
   List.of(
    table("scheme","选配方案代码","X2",SCHEMES),
    table("product","产品机型代码","X3",PRODUCTS),
    table("factory","生产工厂代码","X4",FACTORIES),
    table("brand","品牌代码","X5",BRANDS),
    table("cell","电芯品牌代码","X6",CELLS)));
 }
 private static CodeTable table(String key,String title,String position,Map<String,String> codes){
  List<CodeOption> options=new ArrayList<>();
  new TreeMap<>(codes).forEach((code,label)->options.add(new CodeOption(code,label)));
  return new CodeTable(key,title,position,List.copyOf(options));
 }
 public record SnRule(String version,int length,String pattern,String example,List<Segment> segments,List<CodeTable> codeTables){}
 public record Segment(String position,String name,String rule,String note){}
 public record CodeTable(String key,String title,String position,List<CodeOption> options){}
 public record CodeOption(String code,String label){}
 public record DecodedSn(String normalized,String schemeCode,String schemeName,String productCode,String productName,String factoryCode,String factoryName,String brandCode,String brandName,String cellBrandCode,String cellBrandName,LocalDate manufacturedDate,String serial,String batchCode){}
}
