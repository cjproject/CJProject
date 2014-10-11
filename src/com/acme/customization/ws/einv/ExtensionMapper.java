
/**
 * ExtensionMapper.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.6.2  Built on : Apr 17, 2012 (05:34:40 IST)
 */

        
            package com.acme.customization.ws.einv;
        
            /**
            *  ExtensionMapper class
            */
            @SuppressWarnings({"unchecked","unused"})
        
        public  class ExtensionMapper{

          public static java.lang.Object getTypeObject(java.lang.String namespaceURI,
                                                       java.lang.String typeName,
                                                       javax.xml.stream.XMLStreamReader reader) throws java.lang.Exception{

              
                  if (
                  "http://schemas.datacontract.org/2004/07/eFaturaCoreLib.Common".equals(namespaceURI) &&
                  "SendRecvType".equals(typeName)){
                   
                            return  com.acme.customization.ws.einv.SendRecvType.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://schemas.microsoft.com/2003/10/Serialization/".equals(namespaceURI) &&
                  "guid".equals(typeName)){
                   
                            return  com.acme.customization.ws.einv.Guid.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://schemas.microsoft.com/2003/10/Serialization/".equals(namespaceURI) &&
                  "duration".equals(typeName)){
                   
                            return  com.acme.customization.ws.einv.Duration.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://schemas.microsoft.com/2003/10/Serialization/".equals(namespaceURI) &&
                  "char".equals(typeName)){
                   
                            return  com.acme.customization.ws.einv._char.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://schemas.datacontract.org/2004/07/eFaturaWebService".equals(namespaceURI) &&
                  "LoginType".equals(typeName)){
                   
                            return  com.acme.customization.ws.einv.LoginType.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://schemas.datacontract.org/2004/07/eFaturaCoreLib.Common".equals(namespaceURI) &&
                  "UserListType".equals(typeName)){
                   
                            return  com.acme.customization.ws.einv.UserListType.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://schemas.datacontract.org/2004/07/eFaturaCoreLib.Common".equals(namespaceURI) &&
                  "AppRespStatus".equals(typeName)){
                   
                            return  com.acme.customization.ws.einv.AppRespStatus.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://schemas.datacontract.org/2004/07/eFaturaWebService".equals(namespaceURI) &&
                  "DocumentType".equals(typeName)){
                   
                            return  com.acme.customization.ws.einv.DocumentType.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://schemas.microsoft.com/2003/10/Serialization/Arrays".equals(namespaceURI) &&
                  "ArrayOfstring".equals(typeName)){
                   
                            return  com.acme.customization.ws.einv.ArrayOfstring.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://schemas.datacontract.org/2004/07/eFaturaCoreLib.Common".equals(namespaceURI) &&
                  "InvoiceStatus".equals(typeName)){
                   
                            return  com.acme.customization.ws.einv.InvoiceStatus.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://schemas.datacontract.org/2004/07/eArchiveCoreLib.Common".equals(namespaceURI) &&
                  "EArchiveDocumentTypes".equals(typeName)){
                   
                            return  com.acme.customization.ws.einv.EArchiveDocumentTypes.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://schemas.datacontract.org/2004/07/eFaturaWebService".equals(namespaceURI) &&
                  "base64BinaryData".equals(typeName)){
                   
                            return  com.acme.customization.ws.einv.Base64BinaryData.Factory.parse(reader);
                        

                  }

              
             throw new org.apache.axis2.databinding.ADBException("Unsupported type " + namespaceURI + " " + typeName);
          }

        }
    