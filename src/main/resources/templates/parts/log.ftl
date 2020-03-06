<#macro login path isRegisterForm>
<form action="${path}" method="post">

    <div class="form-group row">
        <label class="col-sm-2 col-form-label"> User Name: </label>
        <div class="col-sm-6">
            <input class="form-control ${(usernameError??)?string('is-invalid', '')}" value="<#if user??>${user.username}</#if>"
                   type="text" name="username" placeholder="User name" />
            <#if usernameError??>
                <div class="invalid-feedback">
                    ${usernameError}
                </div>
            </#if>
        </div>
    </div>

    <div class="form-group row">
        <label class="col-sm-2 col-form-label"> Password: </label>
        <div class="col-sm-6">
            <input class="form-control ${(passwordError??)?string('is-invalid', '')}"
                   type="password" name="password" placeholder="Password" />
            <#if passwordError??>
                <div class="invalid-feedback">
                    ${passwordError}
                </div>
            </#if>
        </div>
    </div>

    <#if isRegisterForm>
        <div class="form-group row">
            <label  class="col-sm-2 col-form-label"> Password: </label>
            <div class="col-sm-6">
                <input class="form-control ${(password2Error??)?string('is-invalid', '')}"
                       type="password" name="password2" placeholder="Retype password" />
                <#if password2Error??>
                <div class="invalid-feedback">
                    ${password2Error}
                </div>
                </#if>
            </div>
        </div>
    </#if>

    <button class="btn btn-primary" type="submit">
        <#if isRegisterForm>Sign Up<#else>Sign In</#if>
    </button>

    <#if !isRegisterForm>
        <a href="/registration" class="ml-2">Sign Up</a>
    </#if>

    <input type="hidden" name="_csrf" value="${_csrf.token}" />
</form>
</#macro>


<#macro logout>
<form action="/logout" method="post" >
    <button class="btn btn-primary" type="submit">Log Out</button>
    <input type="hidden" name="_csrf" value="${_csrf.token}" />
</form>
</#macro>