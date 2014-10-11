

/**
 * PostBoxService.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.6.2  Built on : Apr 17, 2012 (05:33:49 IST)
 */

    package com.acme.customization.ws.einv;

    /*
     *  PostBoxService java interface
     */

    public interface PostBoxService {
          

        /**
          * Auto generated method signature
          * 
                    * @param getInvoice0
                
         */

         
                     public com.acme.customization.ws.einv.GetInvoiceResponse getInvoice(

                        com.acme.customization.ws.einv.GetInvoice getInvoice0)
                        throws java.rmi.RemoteException
             ;

        
         /**
            * Auto generated method signature for Asynchronous Invocations
            * 
                * @param getInvoice0
            
          */
        public void startgetInvoice(

            com.acme.customization.ws.einv.GetInvoice getInvoice0,

            final com.acme.customization.ws.einv.PostBoxServiceCallbackHandler callback)

            throws java.rmi.RemoteException;

     

        /**
          * Auto generated method signature
          * 
                    * @param getApplicationResponse2
                
         */

         
                     public com.acme.customization.ws.einv.GetApplicationResponseResponse getApplicationResponse(

                        com.acme.customization.ws.einv.GetApplicationResponse getApplicationResponse2)
                        throws java.rmi.RemoteException
             ;

        
         /**
            * Auto generated method signature for Asynchronous Invocations
            * 
                * @param getApplicationResponse2
            
          */
        public void startgetApplicationResponse(

            com.acme.customization.ws.einv.GetApplicationResponse getApplicationResponse2,

            final com.acme.customization.ws.einv.PostBoxServiceCallbackHandler callback)

            throws java.rmi.RemoteException;

     

        /**
          * Auto generated method signature
          * 
                    * @param receiveDocument4
                
         */

         
                     public com.acme.customization.ws.einv.ReceiveDocumentResponse receiveDocument(

                        com.acme.customization.ws.einv.ReceiveDocument receiveDocument4)
                        throws java.rmi.RemoteException
             ;

        
         /**
            * Auto generated method signature for Asynchronous Invocations
            * 
                * @param receiveDocument4
            
          */
        public void startreceiveDocument(

            com.acme.customization.ws.einv.ReceiveDocument receiveDocument4,

            final com.acme.customization.ws.einv.PostBoxServiceCallbackHandler callback)

            throws java.rmi.RemoteException;

     

        /**
          * Auto generated method signature
          * 
                    * @param logout6
                
         */

         
                     public com.acme.customization.ws.einv.LogoutResponse logout(

                        com.acme.customization.ws.einv.Logout logout6)
                        throws java.rmi.RemoteException
             ;

        
         /**
            * Auto generated method signature for Asynchronous Invocations
            * 
                * @param logout6
            
          */
        public void startlogout(

            com.acme.customization.ws.einv.Logout logout6,

            final com.acme.customization.ws.einv.PostBoxServiceCallbackHandler callback)

            throws java.rmi.RemoteException;

     

        /**
          * Auto generated method signature
          * 
                    * @param sendInvoice8
                
         */

         
                     public com.acme.customization.ws.einv.SendInvoiceResponse sendInvoice(

                        com.acme.customization.ws.einv.SendInvoice sendInvoice8)
                        throws java.rmi.RemoteException
             ;

        
         /**
            * Auto generated method signature for Asynchronous Invocations
            * 
                * @param sendInvoice8
            
          */
        public void startsendInvoice(

            com.acme.customization.ws.einv.SendInvoice sendInvoice8,

            final com.acme.customization.ws.einv.PostBoxServiceCallbackHandler callback)

            throws java.rmi.RemoteException;

     

        /**
          * Auto generated method signature
          * 
                    * @param sendApplicationResponseEx10
                
         */

         
                     public com.acme.customization.ws.einv.SendApplicationResponseExResponse sendApplicationResponseEx(

                        com.acme.customization.ws.einv.SendApplicationResponseEx sendApplicationResponseEx10)
                        throws java.rmi.RemoteException
             ;

        
         /**
            * Auto generated method signature for Asynchronous Invocations
            * 
                * @param sendApplicationResponseEx10
            
          */
        public void startsendApplicationResponseEx(

            com.acme.customization.ws.einv.SendApplicationResponseEx sendApplicationResponseEx10,

            final com.acme.customization.ws.einv.PostBoxServiceCallbackHandler callback)

            throws java.rmi.RemoteException;

     

        /**
          * Auto generated method signature
          * 
                    * @param sendEnvelopeEx12
                
         */

         
                     public com.acme.customization.ws.einv.SendEnvelopeExResponse sendEnvelopeEx(

                        com.acme.customization.ws.einv.SendEnvelopeEx sendEnvelopeEx12)
                        throws java.rmi.RemoteException
             ;

        
         /**
            * Auto generated method signature for Asynchronous Invocations
            * 
                * @param sendEnvelopeEx12
            
          */
        public void startsendEnvelopeEx(

            com.acme.customization.ws.einv.SendEnvelopeEx sendEnvelopeEx12,

            final com.acme.customization.ws.einv.PostBoxServiceCallbackHandler callback)

            throws java.rmi.RemoteException;

     

        /**
          * Auto generated method signature
          * 
                    * @param sendEArchiveDocument14
                
         */

         
                     public com.acme.customization.ws.einv.SendEArchiveDocumentResponse sendEArchiveDocument(

                        com.acme.customization.ws.einv.SendEArchiveDocument sendEArchiveDocument14)
                        throws java.rmi.RemoteException
             ;

        
         /**
            * Auto generated method signature for Asynchronous Invocations
            * 
                * @param sendEArchiveDocument14
            
          */
        public void startsendEArchiveDocument(

            com.acme.customization.ws.einv.SendEArchiveDocument sendEArchiveDocument14,

            final com.acme.customization.ws.einv.PostBoxServiceCallbackHandler callback)

            throws java.rmi.RemoteException;

     

        /**
          * Auto generated method signature
          * 
                    * @param getInvoiceList16
                
         */

         
                     public com.acme.customization.ws.einv.GetInvoiceListResponse getInvoiceList(

                        com.acme.customization.ws.einv.GetInvoiceList getInvoiceList16)
                        throws java.rmi.RemoteException
             ;

        
         /**
            * Auto generated method signature for Asynchronous Invocations
            * 
                * @param getInvoiceList16
            
          */
        public void startgetInvoiceList(

            com.acme.customization.ws.einv.GetInvoiceList getInvoiceList16,

            final com.acme.customization.ws.einv.PostBoxServiceCallbackHandler callback)

            throws java.rmi.RemoteException;

     

        /**
          * Auto generated method signature
          * 
                    * @param getEnvelopeList18
                
         */

         
                     public com.acme.customization.ws.einv.GetEnvelopeListResponse getEnvelopeList(

                        com.acme.customization.ws.einv.GetEnvelopeList getEnvelopeList18)
                        throws java.rmi.RemoteException
             ;

        
         /**
            * Auto generated method signature for Asynchronous Invocations
            * 
                * @param getEnvelopeList18
            
          */
        public void startgetEnvelopeList(

            com.acme.customization.ws.einv.GetEnvelopeList getEnvelopeList18,

            final com.acme.customization.ws.einv.PostBoxServiceCallbackHandler callback)

            throws java.rmi.RemoteException;

     

        /**
          * Auto generated method signature
          * 
                    * @param aLive20
                
         */

         
                     public com.acme.customization.ws.einv.ALiveResponse aLive(

                        com.acme.customization.ws.einv.ALive aLive20)
                        throws java.rmi.RemoteException
             ;

        
         /**
            * Auto generated method signature for Asynchronous Invocations
            * 
                * @param aLive20
            
          */
        public void startaLive(

            com.acme.customization.ws.einv.ALive aLive20,

            final com.acme.customization.ws.einv.PostBoxServiceCallbackHandler callback)

            throws java.rmi.RemoteException;

     

        /**
          * Auto generated method signature
          * 
                    * @param getApprovalFlowList22
                
         */

         
                     public com.acme.customization.ws.einv.GetApprovalFlowListResponse getApprovalFlowList(

                        com.acme.customization.ws.einv.GetApprovalFlowList getApprovalFlowList22)
                        throws java.rmi.RemoteException
             ;

        
         /**
            * Auto generated method signature for Asynchronous Invocations
            * 
                * @param getApprovalFlowList22
            
          */
        public void startgetApprovalFlowList(

            com.acme.customization.ws.einv.GetApprovalFlowList getApprovalFlowList22,

            final com.acme.customization.ws.einv.PostBoxServiceCallbackHandler callback)

            throws java.rmi.RemoteException;

     

        /**
          * Auto generated method signature
          * 
                    * @param getApprovalFlowRes24
                
         */

         
                     public com.acme.customization.ws.einv.GetApprovalFlowResResponse getApprovalFlowRes(

                        com.acme.customization.ws.einv.GetApprovalFlowRes getApprovalFlowRes24)
                        throws java.rmi.RemoteException
             ;

        
         /**
            * Auto generated method signature for Asynchronous Invocations
            * 
                * @param getApprovalFlowRes24
            
          */
        public void startgetApprovalFlowRes(

            com.acme.customization.ws.einv.GetApprovalFlowRes getApprovalFlowRes24,

            final com.acme.customization.ws.einv.PostBoxServiceCallbackHandler callback)

            throws java.rmi.RemoteException;

     

        /**
          * Auto generated method signature
          * 
                    * @param sendApplicationResponse26
                
         */

         
                     public com.acme.customization.ws.einv.SendApplicationResponseResponse sendApplicationResponse(

                        com.acme.customization.ws.einv.SendApplicationResponse sendApplicationResponse26)
                        throws java.rmi.RemoteException
             ;

        
         /**
            * Auto generated method signature for Asynchronous Invocations
            * 
                * @param sendApplicationResponse26
            
          */
        public void startsendApplicationResponse(

            com.acme.customization.ws.einv.SendApplicationResponse sendApplicationResponse26,

            final com.acme.customization.ws.einv.PostBoxServiceCallbackHandler callback)

            throws java.rmi.RemoteException;

     

        /**
          * Auto generated method signature
          * 
                    * @param ping28
                
         */

         
                     public com.acme.customization.ws.einv.PingResponse ping(

                        com.acme.customization.ws.einv.Ping ping28)
                        throws java.rmi.RemoteException
             ;

        
         /**
            * Auto generated method signature for Asynchronous Invocations
            * 
                * @param ping28
            
          */
        public void startping(

            com.acme.customization.ws.einv.Ping ping28,

            final com.acme.customization.ws.einv.PostBoxServiceCallbackHandler callback)

            throws java.rmi.RemoteException;

     

        /**
          * Auto generated method signature
          * 
                    * @param getEnvelope30
                
         */

         
                     public com.acme.customization.ws.einv.GetEnvelopeResponse getEnvelope(

                        com.acme.customization.ws.einv.GetEnvelope getEnvelope30)
                        throws java.rmi.RemoteException
             ;

        
         /**
            * Auto generated method signature for Asynchronous Invocations
            * 
                * @param getEnvelope30
            
          */
        public void startgetEnvelope(

            com.acme.customization.ws.einv.GetEnvelope getEnvelope30,

            final com.acme.customization.ws.einv.PostBoxServiceCallbackHandler callback)

            throws java.rmi.RemoteException;

     

        /**
          * Auto generated method signature
          * 
                    * @param receiveDone32
                
         */

         
                     public com.acme.customization.ws.einv.ReceiveDoneResponse receiveDone(

                        com.acme.customization.ws.einv.ReceiveDone receiveDone32)
                        throws java.rmi.RemoteException
             ;

        
         /**
            * Auto generated method signature for Asynchronous Invocations
            * 
                * @param receiveDone32
            
          */
        public void startreceiveDone(

            com.acme.customization.ws.einv.ReceiveDone receiveDone32,

            final com.acme.customization.ws.einv.PostBoxServiceCallbackHandler callback)

            throws java.rmi.RemoteException;

     

        /**
          * Auto generated method signature
          * 
                    * @param getUserList34
                
         */

         
                     public com.acme.customization.ws.einv.GetUserListResponse getUserList(

                        com.acme.customization.ws.einv.GetUserList getUserList34)
                        throws java.rmi.RemoteException
             ;

        
         /**
            * Auto generated method signature for Asynchronous Invocations
            * 
                * @param getUserList34
            
          */
        public void startgetUserList(

            com.acme.customization.ws.einv.GetUserList getUserList34,

            final com.acme.customization.ws.einv.PostBoxServiceCallbackHandler callback)

            throws java.rmi.RemoteException;

     

        /**
          * Auto generated method signature
          * 
                    * @param sendEnvelope36
                
         */

         
                     public com.acme.customization.ws.einv.SendEnvelopeResponse sendEnvelope(

                        com.acme.customization.ws.einv.SendEnvelope sendEnvelope36)
                        throws java.rmi.RemoteException
             ;

        
         /**
            * Auto generated method signature for Asynchronous Invocations
            * 
                * @param sendEnvelope36
            
          */
        public void startsendEnvelope(

            com.acme.customization.ws.einv.SendEnvelope sendEnvelope36,

            final com.acme.customization.ws.einv.PostBoxServiceCallbackHandler callback)

            throws java.rmi.RemoteException;

     

        /**
          * Auto generated method signature
          * 
                    * @param login38
                
         */

         
                     public com.acme.customization.ws.einv.LoginResponse login(

                        com.acme.customization.ws.einv.Login login38)
                        throws java.rmi.RemoteException
             ;

        
         /**
            * Auto generated method signature for Asynchronous Invocations
            * 
                * @param login38
            
          */
        public void startlogin(

            com.acme.customization.ws.einv.Login login38,

            final com.acme.customization.ws.einv.PostBoxServiceCallbackHandler callback)

            throws java.rmi.RemoteException;

     

        /**
          * Auto generated method signature
          * 
                    * @param getAppRespStatus40
                
         */

         
                     public com.acme.customization.ws.einv.GetAppRespStatusResponse getAppRespStatus(

                        com.acme.customization.ws.einv.GetAppRespStatus getAppRespStatus40)
                        throws java.rmi.RemoteException
             ;

        
         /**
            * Auto generated method signature for Asynchronous Invocations
            * 
                * @param getAppRespStatus40
            
          */
        public void startgetAppRespStatus(

            com.acme.customization.ws.einv.GetAppRespStatus getAppRespStatus40,

            final com.acme.customization.ws.einv.PostBoxServiceCallbackHandler callback)

            throws java.rmi.RemoteException;

     

        /**
          * Auto generated method signature
          * 
                    * @param getDocumentStatus42
                
         */

         
                     public com.acme.customization.ws.einv.GetDocumentStatusResponse getDocumentStatus(

                        com.acme.customization.ws.einv.GetDocumentStatus getDocumentStatus42)
                        throws java.rmi.RemoteException
             ;

        
         /**
            * Auto generated method signature for Asynchronous Invocations
            * 
                * @param getDocumentStatus42
            
          */
        public void startgetDocumentStatus(

            com.acme.customization.ws.einv.GetDocumentStatus getDocumentStatus42,

            final com.acme.customization.ws.einv.PostBoxServiceCallbackHandler callback)

            throws java.rmi.RemoteException;

     

        /**
          * Auto generated method signature
          * 
                    * @param getInvoiceStatus44
                
         */

         
                     public com.acme.customization.ws.einv.GetInvoiceStatusResponse getInvoiceStatus(

                        com.acme.customization.ws.einv.GetInvoiceStatus getInvoiceStatus44)
                        throws java.rmi.RemoteException
             ;

        
         /**
            * Auto generated method signature for Asynchronous Invocations
            * 
                * @param getInvoiceStatus44
            
          */
        public void startgetInvoiceStatus(

            com.acme.customization.ws.einv.GetInvoiceStatus getInvoiceStatus44,

            final com.acme.customization.ws.einv.PostBoxServiceCallbackHandler callback)

            throws java.rmi.RemoteException;

     

        /**
          * Auto generated method signature
          * 
                    * @param sendInvoiceEx46
                
         */

         
                     public com.acme.customization.ws.einv.SendInvoiceExResponse sendInvoiceEx(

                        com.acme.customization.ws.einv.SendInvoiceEx sendInvoiceEx46)
                        throws java.rmi.RemoteException
             ;

        
         /**
            * Auto generated method signature for Asynchronous Invocations
            * 
                * @param sendInvoiceEx46
            
          */
        public void startsendInvoiceEx(

            com.acme.customization.ws.einv.SendInvoiceEx sendInvoiceEx46,

            final com.acme.customization.ws.einv.PostBoxServiceCallbackHandler callback)

            throws java.rmi.RemoteException;

     

        /**
          * Auto generated method signature
          * 
                    * @param setApprovalFlowId48
                
         */

         
                     public com.acme.customization.ws.einv.SetApprovalFlowIdResponse setApprovalFlowId(

                        com.acme.customization.ws.einv.SetApprovalFlowId setApprovalFlowId48)
                        throws java.rmi.RemoteException
             ;

        
         /**
            * Auto generated method signature for Asynchronous Invocations
            * 
                * @param setApprovalFlowId48
            
          */
        public void startsetApprovalFlowId(

            com.acme.customization.ws.einv.SetApprovalFlowId setApprovalFlowId48,

            final com.acme.customization.ws.einv.PostBoxServiceCallbackHandler callback)

            throws java.rmi.RemoteException;

     

        
       //
       }
    