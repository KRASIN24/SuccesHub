<#ftl output_format="plainText">
SuccessHub — Reset your password

Someone requested a password reset for your account<#if user.username??> (${user.username})</#if>.

Open this link to choose a new password (expires in ${linkExpiration} minutes):

${link}

If you did not request this, you can ignore this email.
