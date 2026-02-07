# Error Handling Rules

## Common Error Types

### Network Errors
- **Connection Timeout**: Retry with exponential backoff
- **DNS Resolution**: Check network connectivity
- **SSL/TLS Errors**: Verify certificate validity
- **Rate Limiting**: Implement delay and retry logic

### Content Errors
- **Video Unavailable**: Content removed, private, or region-locked
- **Age Restricted**: Requires authentication or age verification
- **Copyright Claimed**: Content blocked due to copyright
- **Live Stream**: Cannot download live content

### Format Errors
- **Unsupported Format**: Requested format not available
- **Codec Issues**: Incompatible codec combinations
- **Container Problems**: File container corruption
- **Metadata Missing**: Required metadata unavailable

## Retry Strategy
- **Initial Retry**: Immediate retry for transient errors
- **Exponential Backoff**: 1s, 2s, 4s, 8s delays
- **Maximum Retries**: 3 attempts total
- **Circuit Breaker**: Stop after repeated failures

## Error Classification
- **Fatal**: Cannot recover (private video, deleted content)
- **Retryable**: May succeed later (network issues, temporary blocks)
- **Configurable**: User can adjust settings (format preferences)

## User Communication
- **Clear Messages**: Explain errors in plain language
- **Actionable Advice**: Suggest specific fixes or alternatives
- **Progress Updates**: Show retry attempts and status
- **Summary Reports**: List all errors and resolutions

## Logging Requirements
- **Error Details**: Full stack traces and context
- **Timestamp**: When error occurred
- **Attempt Count**: Number of retry attempts
- **Resolution**: How error was handled

## Fallback Options
- **Alternative Formats**: Try different quality/format combinations
- **Partial Downloads**: Resume interrupted downloads
- **Metadata Only**: Fall back to metadata extraction
- **Skip and Continue**: Continue with remaining items in batch

## Monitoring and Alerts
- **Error Rates**: Track failure percentages
- **Common Issues**: Identify patterns for improvement
- **User Impact**: Measure effect on user experience
- **System Health**: Monitor tool reliability