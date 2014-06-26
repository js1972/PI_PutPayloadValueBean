PI_PutPayloadValueBean
======================

PI Adapter module (can be used as a template) to add element to message - working with SAP's GetPayloadValueBean

This repo is an EJB for a PI adapter module. It works in conjunction with SAP's standard module: GetPayloadValueBean. GetPayloadValueBean allows you to select element values in the payload and store them in the module context.
Using this PI_PutPayloadValueBean we can grab those values and insert them as new elements in the message payload.
This bean does not care what the message payload looks like. It simply adds the new elements under the top-level node.

SAP's version of this (PutPayloadValueBean) is no good because it returns null pointer exceptions when the payload is not what it expects which rules out is use in scenario's where a soap fault could be returned.

To use this module, proceed as follows:

1. Configure SAP's GetPayloadValueBean in the module chain prior to the call-sap-adapter module. See this blog for the module parameters: http://scn.sap.com/community/pi-and-soa-middleware/blog/2013/03/20/insert-value-from-request-message-to-response-using-getpayloadvaluebean-and-putpayloadvaluebean
2. Configure this PI_PutPayloadValueBean after the call-sap-adapter module but before the ResponseOnewayBean (if using a async/sync bridge, which is most likely if you're looking at this module)
3. Set module parameters as: fieldName : \<ELEMENT NAME YOU WANT TO USE\>, fieldVar : \<THE VARIABLE NAME USED IN GETPAYLOADVALUEBEAN\>
 

