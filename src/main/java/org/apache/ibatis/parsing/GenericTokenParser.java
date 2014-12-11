/*
 *    Copyright 2009-2012 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.parsing;

/**
 * 普通标识解析器 解析#{} ${}
 * @author Clinton Begin
 */
public class GenericTokenParser {

  private final String openToken;//开始标识
  private final String closeToken;//结束标识
  private final TokenHandler handler;//token处理器
  //带参构造器 初始化属性
  public GenericTokenParser(String openToken, String closeToken, TokenHandler handler) {
    this.openToken = openToken;
    this.closeToken = closeToken;
    this.handler = handler;
  }

  /**
   * 其实就是将输入字符串中的以openToken和closeToken包裹的内容提取出来 交给 TokenHandler处理一下 
   * 然后再将处理结果拼装到原始字符串中
   * 详见org.apache.ibatis.parsing.GenericTokenParserTest
   * @param text
   * @return
   */
  public String parse(String text) {
    StringBuilder builder = new StringBuilder();
    if (text != null && text.length() > 0) {//如果传入的字符串有值
      char[] src = text.toCharArray();//将字符串转化成字符数组
      int offset = 0;
      int start = text.indexOf(openToken, offset);//传入字符串中是否存在openToken,若存在,返回位置
      while (start > -1) {
        if (start > 0 && src[start - 1] == '\\') {//判断openToken前是否存在转义字符'\'
          // the variable is escaped. remove the backslash.
        //如果openToken前存在转义字符，则当前的openToken不需要处理 对应的closeToken也不需要处理。接着查找下一个openToken
          builder.append(src, offset, start - offset - 1).append(openToken);
          offset = start + openToken.length();//重设offset值
        } else {
          int end = text.indexOf(closeToken, start);
          if (end == -1) {//若不存在closeToken，将剩余内容全部加入到builder中
            builder.append(src, offset, src.length - offset);
            offset = src.length;//重设offset
          } else {
            builder.append(src, offset, start - offset);
            offset = start + openToken.length();
            String content = new String(src, offset, end - offset);//取出openToken与closeToken包裹的内容
            builder.append(handler.handleToken(content));//调用TokenHandler处理取出的内容
            offset = end + closeToken.length();//重设offset
          }
        }
        start = text.indexOf(openToken, offset);//开始从offset开始查找下一个openToken 开启新的循环
      }
      //1.若最后一个closeToken后还有内容 ，将剩余内容追加到builder 
      //2.或者 输入字符串中压根就没有openToken
      if (offset < src.length) {
        builder.append(src, offset, src.length - offset);
      }
    }
    return builder.toString();
  }

}
