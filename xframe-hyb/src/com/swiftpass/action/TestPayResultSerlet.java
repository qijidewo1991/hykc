package com.swiftpass.action;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.swiftpass.config.SwiftpassConfig;
import com.swiftpass.util.SignUtils;
import com.swiftpass.util.XmlUtils;

/**
 * <涓�彞璇濆姛鑳界畝杩�
 * <鍔熻兘璇︾粏鎻忚堪>閫氱煡
 * 
 * @author  Administrator
 * @version  [鐗堟湰鍙� 2014-8-28]
 * @see  [鐩稿叧绫�鏂规硶]
 * @since  [浜у搧/妯″潡鐗堟湰]
 */
public class TestPayResultSerlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            req.setCharacterEncoding("utf-8");
            resp.setCharacterEncoding("utf-8");
            resp.setHeader("Content-type", "text/html;charset=UTF-8");
            String resString = XmlUtils.parseRequst(req);
            System.out.println("通知内容:" + resString);
            
            String respString = "fail";
            if(resString != null && !"".equals(resString)){
                Map<String,String> map = XmlUtils.toMap(resString.getBytes(), "utf-8");
                String res = XmlUtils.toXml(map);
                System.out.println("通知内容:"+ res);
                if(map.containsKey("sign")){
                    if(!SignUtils.checkParam(map, SwiftpassConfig.key)){
                        res = "验证签名不通过";
                        respString = "fail";
                    }else{
                        String status = map.get("status");
                        if(status != null && "0".equals(status)){
                            String result_code = map.get("result_code");
                            if(result_code != null && "0".equals(result_code)){
                               //姝ゅ鍙互鍦ㄦ坊鍔犵浉鍏冲鐞嗕笟鍔★紝鏍￠獙閫氱煡鍙傛暟涓殑鍟嗘埛璁㈠崟鍙穙ut_trade_no鍜岄噾棰漷otal_fee鏄惁鍜屽晢鎴蜂笟鍔＄郴缁熺殑鍗曞彿鍜岄噾棰濇槸鍚︿竴鑷达紝涓�嚧鍚庢柟鍙洿鏂版暟鎹簱琛ㄤ腑鐨勮褰曘� 
                                
                            } 
                        } 
                        respString = "success";
                    }
                }
            }
            resp.getWriter().write(respString);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
