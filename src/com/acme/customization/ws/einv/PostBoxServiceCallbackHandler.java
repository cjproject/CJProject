
/**
 * PostBoxServiceCallbackHandler.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.6.2  Built on : Apr 17, 2012 (05:33:49 IST)
 */

    package com.acme.customization.ws.einv;

    /**
     *  PostBoxServiceCallbackHandler Callback class, Users can extend this class and implement
     *  their own receiveResult and receiveError methods.
     */
    public abstract class PostBoxServiceCallbackHandler{



    protected Object clientData;

    /**
    * User can pass in any object that needs to be accessed once the NonBlocking
    * Web service call is finished and appropriate method of this CallBack is called.
    * @param clientData Object mechanism by which the user can pass in user data
    * that will be avilable at the time this callback is called.
    */
    public PostBoxServiceCallbackHandler(Object clientData){
        this.clientData = clientData;
    }

    /**
    * Please use this constructor if you don't want to set any clientData
    */
    public PostBoxServiceCallbackHandler(){
        this.clientData = null;
    }

    /**
     * Get the client data
     */

     public Object getClientData() {
        return clientData;
     }

        
           /**
            * auto generated Axis2 call back method for getInvoice method
            * override this method for handling normal response from getInvoice operation
            */
           public void receiveResultgetInvoice(
                    com.acme.customization.ws.einv.GetInvoiceResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from getInvoice operation
           */
            public void receiveErrorgetInvoice(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for getApplicationResponse method
            * override this method for handling normal response from getApplicationResponse operation
            */
           public void receiveResultgetApplicationResponse(
                    com.acme.customization.ws.einv.GetApplicationResponseResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from getApplicationResponse operation
           */
            public void receiveErrorgetApplicationResponse(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for receiveDocument method
            * override this method for handling normal response from receiveDocument operation
            */
           public void receiveResultreceiveDocument(
                    com.acme.customization.ws.einv.ReceiveDocumentResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from receiveDocument operation
           */
            public void receiveErrorreceiveDocument(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for logout method
            * override this method for handling normal response from logout operation
            */
           public void receiveResultlogout(
                    com.acme.customization.ws.einv.LogoutResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from logout operation
           */
            public void receiveErrorlogout(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for sendInvoice method
            * override this method for handling normal response from sendInvoice operation
            */
           public void receiveResultsendInvoice(
                    com.acme.customization.ws.einv.SendInvoiceResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from sendInvoice operation
           */
            public void receiveErrorsendInvoice(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for sendApplicationResponseEx method
            * override this method for handling normal response from sendApplicationResponseEx operation
            */
           public void receiveResultsendApplicationResponseEx(
                    com.acme.customization.ws.einv.SendApplicationResponseExResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from sendApplicationResponseEx operation
           */
            public void receiveErrorsendApplicationResponseEx(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for sendEnvelopeEx method
            * override this method for handling normal response from sendEnvelopeEx operation
            */
           public void receiveResultsendEnvelopeEx(
                    com.acme.customization.ws.einv.SendEnvelopeExResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from sendEnvelopeEx operation
           */
            public void receiveErrorsendEnvelopeEx(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for sendEArchiveDocument method
            * override this method for handling normal response from sendEArchiveDocument operation
            */
           public void receiveResultsendEArchiveDocument(
                    com.acme.customization.ws.einv.SendEArchiveDocumentResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from sendEArchiveDocument operation
           */
            public void receiveErrorsendEArchiveDocument(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for getInvoiceList method
            * override this method for handling normal response from getInvoiceList operation
            */
           public void receiveResultgetInvoiceList(
                    com.acme.customization.ws.einv.GetInvoiceListResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from getInvoiceList operation
           */
            public void receiveErrorgetInvoiceList(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for getEnvelopeList method
            * override this method for handling normal response from getEnvelopeList operation
            */
           public void receiveResultgetEnvelopeList(
                    com.acme.customization.ws.einv.GetEnvelopeListResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from getEnvelopeList operation
           */
            public void receiveErrorgetEnvelopeList(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for aLive method
            * override this method for handling normal response from aLive operation
            */
           public void receiveResultaLive(
                    com.acme.customization.ws.einv.ALiveResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from aLive operation
           */
            public void receiveErroraLive(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for getApprovalFlowList method
            * override this method for handling normal response from getApprovalFlowList operation
            */
           public void receiveResultgetApprovalFlowList(
                    com.acme.customization.ws.einv.GetApprovalFlowListResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from getApprovalFlowList operation
           */
            public void receiveErrorgetApprovalFlowList(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for getApprovalFlowRes method
            * override this method for handling normal response from getApprovalFlowRes operation
            */
           public void receiveResultgetApprovalFlowRes(
                    com.acme.customization.ws.einv.GetApprovalFlowResResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from getApprovalFlowRes operation
           */
            public void receiveErrorgetApprovalFlowRes(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for sendApplicationResponse method
            * override this method for handling normal response from sendApplicationResponse operation
            */
           public void receiveResultsendApplicationResponse(
                    com.acme.customization.ws.einv.SendApplicationResponseResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from sendApplicationResponse operation
           */
            public void receiveErrorsendApplicationResponse(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for ping method
            * override this method for handling normal response from ping operation
            */
           public void receiveResultping(
                    com.acme.customization.ws.einv.PingResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from ping operation
           */
            public void receiveErrorping(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for getEnvelope method
            * override this method for handling normal response from getEnvelope operation
            */
           public void receiveResultgetEnvelope(
                    com.acme.customization.ws.einv.GetEnvelopeResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from getEnvelope operation
           */
            public void receiveErrorgetEnvelope(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for receiveDone method
            * override this method for handling normal response from receiveDone operation
            */
           public void receiveResultreceiveDone(
                    com.acme.customization.ws.einv.ReceiveDoneResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from receiveDone operation
           */
            public void receiveErrorreceiveDone(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for getUserList method
            * override this method for handling normal response from getUserList operation
            */
           public void receiveResultgetUserList(
                    com.acme.customization.ws.einv.GetUserListResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from getUserList operation
           */
            public void receiveErrorgetUserList(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for sendEnvelope method
            * override this method for handling normal response from sendEnvelope operation
            */
           public void receiveResultsendEnvelope(
                    com.acme.customization.ws.einv.SendEnvelopeResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from sendEnvelope operation
           */
            public void receiveErrorsendEnvelope(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for login method
            * override this method for handling normal response from login operation
            */
           public void receiveResultlogin(
                    com.acme.customization.ws.einv.LoginResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from login operation
           */
            public void receiveErrorlogin(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for getAppRespStatus method
            * override this method for handling normal response from getAppRespStatus operation
            */
           public void receiveResultgetAppRespStatus(
                    com.acme.customization.ws.einv.GetAppRespStatusResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from getAppRespStatus operation
           */
            public void receiveErrorgetAppRespStatus(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for getDocumentStatus method
            * override this method for handling normal response from getDocumentStatus operation
            */
           public void receiveResultgetDocumentStatus(
                    com.acme.customization.ws.einv.GetDocumentStatusResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from getDocumentStatus operation
           */
            public void receiveErrorgetDocumentStatus(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for getInvoiceStatus method
            * override this method for handling normal response from getInvoiceStatus operation
            */
           public void receiveResultgetInvoiceStatus(
                    com.acme.customization.ws.einv.GetInvoiceStatusResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from getInvoiceStatus operation
           */
            public void receiveErrorgetInvoiceStatus(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for sendInvoiceEx method
            * override this method for handling normal response from sendInvoiceEx operation
            */
           public void receiveResultsendInvoiceEx(
                    com.acme.customization.ws.einv.SendInvoiceExResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from sendInvoiceEx operation
           */
            public void receiveErrorsendInvoiceEx(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for setApprovalFlowId method
            * override this method for handling normal response from setApprovalFlowId operation
            */
           public void receiveResultsetApprovalFlowId(
                    com.acme.customization.ws.einv.SetApprovalFlowIdResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from setApprovalFlowId operation
           */
            public void receiveErrorsetApprovalFlowId(java.lang.Exception e) {
            }
                


    }
    