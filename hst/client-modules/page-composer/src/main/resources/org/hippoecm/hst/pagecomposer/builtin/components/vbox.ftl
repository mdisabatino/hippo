<#--
  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)

  Licensed under the Apache License, Version 2.0 (the  "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS"
  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License. -->

<#assign hst=JspTaglibs["http://www.hippoecm.org/jsp/hst/core"]>

<@hst.defineObjects/>
<#if hstRequest.requestContext.cmsRequest>
    <#assign containerClass = " class=\"hst-container\"">
    <#assign containerItemClass = " class=\"hst-container-item\"">
<#else>
    <#assign containerClass = "">
    <#assign containerItemClass = "">
</#if>
<div${containerClass}>
  <#list hstResponseChildContentNames as childContentName>
  <div${containerItemClass}>
    <@hst.include ref="${childContentName}"/>
  </div>
  </#list>
</div>