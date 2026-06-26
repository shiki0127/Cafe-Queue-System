param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$StudentId = "stu_1001"
)

$ErrorActionPreference = "Stop"

function Invoke-CafeQueueJson {
    param(
        [string]$Method,
        [string]$Path,
        [object]$Body = $null,
        [string]$Token = $null
    )

    $headers = @{}
    if ($Token) {
        $headers["Authorization"] = "Bearer $Token"
    }

    $params = @{
        Method = $Method
        Uri = "$BaseUrl$Path"
        Headers = $headers
        ContentType = "application/json"
    }
    if ($null -ne $Body) {
        $params.Body = ($Body | ConvertTo-Json -Depth 10)
    }

    $response = Invoke-RestMethod @params
    if (-not $response.success) {
        throw "Request failed: $Path $($response.code) $($response.message)"
    }
    return $response.data
}

$tokenResponse = Invoke-CafeQueueJson -Method "POST" -Path "/api/auth/token" -Body @{
    studentId = $StudentId
    role = "STUDENT"
}
$token = $tokenResponse.accessToken

$coupon = Invoke-CafeQueueJson -Method "POST" -Path "/api/coupons/issue" -Token $token -Body @{
    studentId = $StudentId
    templateCode = "WELCOME_3"
}

$order = Invoke-CafeQueueJson -Method "POST" -Path "/api/orders" -Token $token -Body @{
    studentId = $StudentId
    machineId = "MACHINE_A01"
    recipeCode = "LATTE"
    couponCode = $coupon.couponCode
}

$paid = Invoke-CafeQueueJson -Method "POST" -Path "/api/orders/$($order.orderId)/payment-callback" -Token $token -Body @{
    callbackId = "pay_$($order.orderId)"
    payload = "{`"channel`":`"mock`",`"amount`":18}"
}

$notifications = Invoke-CafeQueueJson -Method "GET" -Path "/api/notifications/students/$StudentId" -Token $token

[PSCustomObject]@{
    tokenIssued = [bool]$token
    couponCode = $coupon.couponCode
    orderId = $paid.orderId
    status = $paid.status
    queueTicketId = $paid.queueTicketId
    deviceCommandId = $paid.deviceCommandId
    notificationCount = $notifications.Count
} | ConvertTo-Json -Depth 10
