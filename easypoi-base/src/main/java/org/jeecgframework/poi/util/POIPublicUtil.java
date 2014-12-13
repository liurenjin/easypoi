package org.jeecgframework.poi.util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.poi.POIXMLDocumentPart;
import org.apache.poi.hssf.usermodel.HSSFClientAnchor;
import org.apache.poi.hssf.usermodel.HSSFPicture;
import org.apache.poi.hssf.usermodel.HSSFPictureData;
import org.apache.poi.hssf.usermodel.HSSFShape;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.PictureData;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFPicture;
import org.apache.poi.xssf.usermodel.XSSFShape;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.jeecgframework.poi.excel.annotation.Excel;
import org.jeecgframework.poi.excel.annotation.ExcelCollection;
import org.jeecgframework.poi.excel.annotation.ExcelEntity;
import org.jeecgframework.poi.excel.annotation.ExcelIgnore;
import org.jeecgframework.poi.excel.entity.vo.PoiBaseConstants;
import org.jeecgframework.poi.word.entity.WordImageEntity;
import org.jeecgframework.poi.word.entity.params.ExcelListEntity;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTMarker;

public class POIPublicUtil {

    private POIPublicUtil() {

    }

    /**
     * 彻底创建一个对象
     * 
     * @param clazz
     * @return
     */
    public static Object createObject(Class<?> clazz, String targetId) {
        Object obj = null;
        Method setMethod;
        try {
            obj = clazz.newInstance();
            Field[] fields = getClassFields(clazz);
            for (Field field : fields) {
                if (isNotUserExcelUserThis(null, field, targetId)) {
                    continue;
                }
                if (isCollection(field.getType())) {
                    ExcelCollection collection = field.getAnnotation(ExcelCollection.class);
                    setMethod = getMethod(field.getName(), clazz, field.getType());
                    setMethod.invoke(obj, collection.type().newInstance());
                } else if (!isJavaClass(field)) {
                    setMethod = getMethod(field.getName(), clazz, field.getType());
                    setMethod.invoke(obj, createObject(field.getType(), targetId));
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("创建对象异常");
        }
        return obj;

    }

    /**
     * 获取class的 包括父类的
     * 
     * @param clazz
     * @return
     */
    public static Field[] getClassFields(Class<?> clazz) {
        List<Field> list = new ArrayList<Field>();
        Field[] fields;
        do {
            fields = clazz.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                list.add(fields[i]);
            }
            clazz = clazz.getSuperclass();
        } while (clazz != Object.class && clazz != null);
        return list.toArray(fields);
    }

    /**
     * @param photoByte
     * @return
     */
    public static String getFileExtendName(byte[] photoByte) {
        String strFileExtendName = "JPG";
        if ((photoByte[0] == 71) && (photoByte[1] == 73) && (photoByte[2] == 70)
            && (photoByte[3] == 56) && ((photoByte[4] == 55) || (photoByte[4] == 57))
            && (photoByte[5] == 97)) {
            strFileExtendName = "GIF";
        } else if ((photoByte[6] == 74) && (photoByte[7] == 70) && (photoByte[8] == 73)
                   && (photoByte[9] == 70)) {
            strFileExtendName = "JPG";
        } else if ((photoByte[0] == 66) && (photoByte[1] == 77)) {
            strFileExtendName = "BMP";
        } else if ((photoByte[1] == 80) && (photoByte[2] == 78) && (photoByte[3] == 71)) {
            strFileExtendName = "PNG";
        }
        return strFileExtendName;
    }

    /**
     * 获取GET方法
     * 
     * @param name
     * @param pojoClass
     * @return
     * @throws Exception
     */
    public static Method getMethod(String name, Class<?> pojoClass) throws Exception {
        StringBuffer getMethodName = new StringBuffer(PoiBaseConstants.GET);
        getMethodName.append(name.substring(0, 1).toUpperCase());
        getMethodName.append(name.substring(1));
        Method method = null;
        try {
            method = pojoClass.getMethod(getMethodName.toString(), new Class[] {});
        } catch (Exception e) {
            method = pojoClass.getMethod(
                getMethodName.toString().replace(PoiBaseConstants.GET, PoiBaseConstants.IS),
                new Class[] {});
        }
        return method;
    }

    /**
     * 获取SET方法
     * 
     * @param name
     * @param pojoClass
     * @param type
     * @return
     * @throws Exception
     */
    public static Method getMethod(String name, Class<?> pojoClass, Class<?> type) throws Exception {
        StringBuffer getMethodName = new StringBuffer(PoiBaseConstants.SET);
        getMethodName.append(name.substring(0, 1).toUpperCase());
        getMethodName.append(name.substring(1));
        return pojoClass.getMethod(getMethodName.toString(), new Class[] { type });
    }

    /**
     * 获取Excel2003图片
     * 
     * @param sheet
     *            当前sheet对象
     * @param workbook
     *            工作簿对象
     * @return Map key:图片单元格索引（1_1）String，value:图片流PictureData
     */
    public static Map<String, PictureData> getSheetPictrues03(HSSFSheet sheet, HSSFWorkbook workbook) {
        Map<String, PictureData> sheetIndexPicMap = new HashMap<String, PictureData>();
        List<HSSFPictureData> pictures = workbook.getAllPictures();
        if (pictures.size() != 0) {
            for (HSSFShape shape : sheet.getDrawingPatriarch().getChildren()) {
                HSSFClientAnchor anchor = (HSSFClientAnchor) shape.getAnchor();
                if (shape instanceof HSSFPicture) {
                    HSSFPicture pic = (HSSFPicture) shape;
                    int pictureIndex = pic.getPictureIndex() - 1;
                    HSSFPictureData picData = pictures.get(pictureIndex);
                    String picIndex = String.valueOf(anchor.getRow1()) + "_"
                                      + String.valueOf(anchor.getCol1());
                    sheetIndexPicMap.put(picIndex, picData);
                }
            }
            return sheetIndexPicMap;
        } else {
            return null;
        }
    }

    /**
     * 获取Excel2007图片
     * 
     * @param sheet
     *            当前sheet对象
     * @param workbook
     *            工作簿对象
     * @return Map key:图片单元格索引（1_1）String，value:图片流PictureData
     */
    public static Map<String, PictureData> getSheetPictrues07(XSSFSheet sheet, XSSFWorkbook workbook) {
        Map<String, PictureData> sheetIndexPicMap = new HashMap<String, PictureData>();
        for (POIXMLDocumentPart dr : sheet.getRelations()) {
            if (dr instanceof XSSFDrawing) {
                XSSFDrawing drawing = (XSSFDrawing) dr;
                List<XSSFShape> shapes = drawing.getShapes();
                for (XSSFShape shape : shapes) {
                    XSSFPicture pic = (XSSFPicture) shape;
                    XSSFClientAnchor anchor = pic.getPreferredSize();
                    CTMarker ctMarker = anchor.getFrom();
                    String picIndex = ctMarker.getRow() + "_" + ctMarker.getCol();
                    sheetIndexPicMap.put(picIndex, pic.getPictureData());
                }
            }
        }
        return sheetIndexPicMap;
    }

    public static String getWebRootPath(String filePath) {
        // 这个path还是要测试的
        String path = POIPublicUtil.class.getClassLoader().getResource("").getPath() + filePath;
        path = path.replace("WEB-INF/classes/", "");
        path = path.replace("file:/", "");
        return path;
    }

    /**
     * 判断是不是集合的实现类
     * 
     * @param clazz
     * @return
     */
    public static boolean isCollection(Class<?> clazz) {
        return Collection.class.isAssignableFrom(clazz);
    }

    /**
     * 是不是java基础类
     * 
     * @param field
     * @return
     */
    public static boolean isJavaClass(Field field) {
        Class<?> fieldType = field.getType();
        boolean isBaseClass = false;
        if (fieldType.isArray()) {
            isBaseClass = false;
        } else if (fieldType.isPrimitive() || fieldType.getPackage() == null
                   || fieldType.getPackage().getName().equals("java.lang")
                   || fieldType.getPackage().getName().equals("java.math")
                   || fieldType.getPackage().getName().equals("java.util")) {
            isBaseClass = true;
        }
        return isBaseClass;
    }

    /**
     * 判断是否不要在这个excel操作中
     * 
     * @param
     * @param field
     * @param targetId
     * @return
     */
    public static boolean isNotUserExcelUserThis(List<String> exclusionsList, Field field,
                                                 String targetId) {
        boolean boo = true;
        if (field.getAnnotation(ExcelIgnore.class) != null) {
            boo = true;
        } else if (boo
                   && field.getAnnotation(ExcelCollection.class) != null
                   && isUseInThis(field.getAnnotation(ExcelCollection.class).name(), targetId)
                   && (exclusionsList == null || !exclusionsList.contains(field.getAnnotation(
                       ExcelCollection.class).name()))) {
            boo = false;
        } else if (boo
                   && field.getAnnotation(Excel.class) != null
                   && isUseInThis(field.getAnnotation(Excel.class).name(), targetId)
                   && (exclusionsList == null || !exclusionsList.contains(field.getAnnotation(
                       Excel.class).name()))) {
            boo = false;
        } else if (boo
                   && field.getAnnotation(ExcelEntity.class) != null
                   && isUseInThis(field.getAnnotation(ExcelEntity.class).name(), targetId)
                   && (exclusionsList == null || !exclusionsList.contains(field.getAnnotation(
                       ExcelEntity.class).name()))) {
            boo = false;
        }
        return boo;
    }

    /**
     * 判断是不是使用
     * 
     * @param exportName
     * @param targetId
     * @return
     */
    private static boolean isUseInThis(String exportName, String targetId) {
        return targetId == null || exportName.equals("") || exportName.indexOf("_") < 0
               || exportName.indexOf(targetId) != -1;
    }

    private static Integer getImageType(String type) {
        if (type.equalsIgnoreCase("JPG") || type.equalsIgnoreCase("JPEG")) {
            return XWPFDocument.PICTURE_TYPE_JPEG;
        }
        if (type.equalsIgnoreCase("GIF")) {
            return XWPFDocument.PICTURE_TYPE_GIF;
        }
        if (type.equalsIgnoreCase("BMP")) {
            return XWPFDocument.PICTURE_TYPE_GIF;
        }
        if (type.equalsIgnoreCase("PNG")) {
            return XWPFDocument.PICTURE_TYPE_PNG;
        }
        return XWPFDocument.PICTURE_TYPE_JPEG;
    }

    /**
     * 返回流和图片类型
     *@Author JueYue
     *@date   2013-11-20
     *@param obj
     *@return  (byte[]) isAndType[0],(Integer)isAndType[1]
     * @throws Exception 
     */
    public static Object[] getIsAndType(WordImageEntity entity) throws Exception {
        Object[] result = new Object[2];
        String type;
        if (entity.getType().equals(WordImageEntity.URL)) {
            ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
            BufferedImage bufferImg;
            String path = Thread.currentThread().getContextClassLoader().getResource("").toURI()
                .getPath()
                          + entity.getUrl();
            path = path.replace("WEB-INF/classes/", "");
            path = path.replace("file:/", "");
            bufferImg = ImageIO.read(new File(path));
            ImageIO.write(
                bufferImg,
                entity.getUrl().substring(entity.getUrl().indexOf(".") + 1,
                    entity.getUrl().length()), byteArrayOut);
            result[0] = byteArrayOut.toByteArray();
            type = entity.getUrl().split("/.")[entity.getUrl().split("/.").length - 1];
        } else {
            result[0] = entity.getData();
            type = POIPublicUtil.getFileExtendName(entity.getData());
        }
        result[1] = getImageType(type);
        return result;
    }

    /**
     * 获取参数值
     * 
     * @param params
     * @param map
     * @return
     */
    private static Object getParamsValue(String params, Map<String, Object> map) throws Exception {
        if (params.indexOf(".") != -1) {
            String[] paramsArr = params.split("\\.");
            return getValueDoWhile(map.get(paramsArr[0]), paramsArr, 1);
        }
        return map.containsKey(params) ? map.get(params) : "";
    }

    /**
     * 解析数据
     * 
     * @Author JueYue
     * @date 2013-11-16
     * @return
     */
    public static Object getRealValue(String currentText, Map<String, Object> map) throws Exception {
        String params = "";
        while (currentText.indexOf("{{") != -1) {
            params = currentText
                .substring(currentText.indexOf("{{") + 2, currentText.indexOf("}}"));
            Object obj = getParamsValue(params.trim(), map);
            //判断图片或者是集合

            if (obj instanceof WordImageEntity || obj instanceof List
                || obj instanceof ExcelListEntity) {
                return obj;
            } else {
                currentText = currentText.replace("{{" + params + "}}", obj.toString());
            }
        }
        return currentText;
    }

    /**
     * 通过遍历过去对象值
     * 
     * @param object
     * @param paramsArr
     * @param index
     * @return
     * @throws Exception
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    @SuppressWarnings("rawtypes")
    public static Object getValueDoWhile(Object object, String[] paramsArr, int index)
                                                                                      throws Exception {
        if (object == null) {
            return "";
        }
        if (object instanceof WordImageEntity) {
            return object;
        }
        if (object instanceof Map) {
            object = ((Map) object).get(paramsArr[index]);
        } else {
            object = POIPublicUtil.getMethod(paramsArr[index], object.getClass()).invoke(object,
                new Object[] {});
        }
        return (index == paramsArr.length - 1) ? (object == null ? "" : object) : getValueDoWhile(
            object, paramsArr, ++index);
    }

}
