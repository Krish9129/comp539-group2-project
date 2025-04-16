import React, { useState, useEffect } from 'react';
import { Modal, Button, Spinner, Row, Col, Card, ButtonGroup } from 'react-bootstrap';
import { FaChartBar, FaCalendarAlt, FaMousePointer, FaCalendarWeek, FaCalendarDay } from 'react-icons/fa';
import { 
  Chart as ChartJS, 
  CategoryScale, 
  LinearScale, 
  BarElement, 
  Title, 
  Tooltip, 
  Legend,
  ArcElement,
  PointElement,
  LineElement
} from 'chart.js';
import { Pie, Line } from 'react-chartjs-2';
import DatePicker from 'react-datepicker';
import "react-datepicker/dist/react-datepicker.css";
import urlService from '../services/urlService';
import './UrlAnalytics.css';

// Register ChartJS components
ChartJS.register(
  CategoryScale, 
  LinearScale, 
  BarElement, 
  Title, 
  Tooltip, 
  Legend,
  ArcElement,
  PointElement,
  LineElement
);

const UrlAnalytics = ({ urlId }) => {
  // State for modal visibility
  const [showModal, setShowModal] = useState(false);
  
  // State for analytics data
  const [analyticsData, setAnalyticsData] = useState(null);
  
  // State for date selection
  const [selectedDate, setSelectedDate] = useState(new Date());
  
  // State for time range selection
  const [timeRange, setTimeRange] = useState('daily');
  
  // Loading and error states
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // Get the shortId from the urlId
  // In this component, urlId is actually the shortId from the URL object
  const shortId = urlId;

  // Format date for API request - fixed to avoid timezone issues
  const formatDateForApi = (date) => {
    // Use the date directly without timezone conversion
    // Create the date in local time zone format to avoid date shift
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0'); // Month is 0-indexed, so add 1
    const day = String(date.getDate()).padStart(2, '0');
    
    // Use YYYY-MM-DD format without timezone conversion
    return `${year}-${month}-${day}`;
  };

  // Fetch analytics data
  const fetchAnalytics = async () => {
    setLoading(true);
    setError('');
    
    try {
      const formattedDate = formatDateForApi(selectedDate);
      
      // Use shortId instead of urlId for the API call
      const data = await urlService.getUrlAnalytics(shortId, formattedDate, timeRange);
      setAnalyticsData(data);
      
      // Debug log to check the received data
      console.log("Received analytics data:", data);
    } catch (error) {
      console.error('Error fetching analytics:', error);
      setError('Failed to load analytics data. Please try again later.');
    } finally {
      setLoading(false);
    }
  };

  // When modal is opened or date changes or timeRange changes, fetch data
  useEffect(() => {
    if (showModal) {
      fetchAnalytics();
    }
  }, [showModal, selectedDate, timeRange]);

  // Handle date change
  const handleDateChange = (date) => {
    // Update the date state - this will trigger the useEffect
    setSelectedDate(date);
  };

  // Handle time range change
  const handleTimeRangeChange = (range) => {
    setTimeRange(range);
  };

  // Get colors for charts
  const getColors = (count) => {
    const baseColors = [
      'rgba(54, 162, 235, 0.7)', // Blue
      'rgba(75, 192, 192, 0.7)', // Teal
      'rgba(255, 99, 132, 0.7)', // Red
      'rgba(255, 206, 86, 0.7)', // Yellow
      'rgba(153, 102, 255, 0.7)', // Purple
      'rgba(255, 159, 64, 0.7)', // Orange
      'rgba(199, 199, 199, 0.7)', // Gray
      'rgba(83, 102, 255, 0.7)', // Indigo
      'rgba(255, 99, 255, 0.7)', // Pink
      'rgba(139, 69, 19, 0.7)', // Brown
    ];
    
    // If we need more colors than in our base set, create variations
    if (count > baseColors.length) {
      const extraColors = [];
      for (let i = 0; i < count - baseColors.length; i++) {
        const idx = i % baseColors.length;
        // Create a variation by adjusting opacity
        const color = baseColors[idx].replace('0.7', `${0.3 + (i / count)}`);
        extraColors.push(color);
      }
      return [...baseColors, ...extraColors];
    }
    
    return baseColors.slice(0, count);
  };

  // Prepare chart data
  const prepareChartData = () => {
    if (!analyticsData) return null;

    // Get appropriate labels and data based on timeRange
    const getTimeLabelsAndData = () => {
      if (timeRange === 'daily') {
        // For daily: fixed 24 hours
        return {
          labels: Array.from({ length: 24 }, (_, i) => `${i}:00`),
          data: Object.values(analyticsData.clicks_per_hour || {})
        };
      } else if (timeRange === 'weekly') {
        // For weekly: get actual week start dates from the data
        const weekData = analyticsData.clicks_per_week || {};
        const sortedKeys = Object.keys(weekData).sort();
        
        // Format the date labels to be more readable (MM/DD)
        const formattedLabels = sortedKeys.map(dateStr => {
          const date = new Date(dateStr);
          return `${(date.getMonth() + 1).toString().padStart(2, '0')}/${date.getDate().toString().padStart(2, '0')}`;
        });
        
        return {
          labels: formattedLabels,
          data: sortedKeys.map(key => weekData[key])
        };
      } else if (timeRange === 'monthly') {
        // For monthly: get actual months from the data
        const monthData = analyticsData.clicks_per_month || {};
        const sortedKeys = Object.keys(monthData).sort();
        
        // Format the month labels to be more readable (MMM YYYY)
        const formattedLabels = sortedKeys.map(monthStr => {
          const [year, month] = monthStr.split('-');
          const date = new Date(parseInt(year), parseInt(month) - 1, 1);
          return date.toLocaleDateString('en-US', { month: 'short', year: 'numeric' });
        });
        
        return {
          labels: formattedLabels,
          data: sortedKeys.map(key => monthData[key])
        };
      }
      return { labels: [], data: [] };
    };

    const { labels, data } = getTimeLabelsAndData();

    // Clicks Distribution (per hour, day, or month)
    const clicksDistributionData = {
      labels: labels,
      datasets: [
        {
          label: 'Clicks',
          data: data,
          backgroundColor: 'rgba(54, 162, 235, 0.5)',
          borderColor: 'rgba(54, 162, 235, 1)',
          borderWidth: 1,
        },
      ],
    };
    
    // Device Distribution
    const deviceLabels = Object.keys(analyticsData.device_distribution || {});
    const deviceData = {
      labels: deviceLabels,
      datasets: [
        {
          data: Object.values(analyticsData.device_distribution || {}),
          backgroundColor: getColors(deviceLabels.length),
          borderWidth: 1,
        },
      ],
    };
    
    // Country Distribution
    const countryLabels = Object.keys(analyticsData.country_distribution || {});
    const countryData = {
      labels: countryLabels,
      datasets: [
        {
          data: Object.values(analyticsData.country_distribution || {}),
          backgroundColor: getColors(countryLabels.length),
          borderWidth: 1,
        },
      ],
    };
    
    // Browser Distribution
    const browserLabels = Object.keys(analyticsData.browser_distribution || {});
    const browserData = {
      labels: browserLabels,
      datasets: [
        {
          data: Object.values(analyticsData.browser_distribution || {}),
          backgroundColor: getColors(browserLabels.length),
          borderWidth: 1,
        },
      ],
    };
    
    return {
      clicksDistribution: clicksDistributionData,
      deviceDistribution: deviceData,
      countryDistribution: countryData,
      browserDistribution: browserData,
    };
  };

  const chartData = prepareChartData();

  // Chart options
  const lineOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'top',
      },
      title: {
        display: true,
        text: timeRange === 'daily' ? 'Daily Click Distribution' : 
              timeRange === 'weekly' ? 'Weekly Click Distribution' : 
              'Monthly Click Distribution',
        font: {
          size: 16
        }
      },
      tooltip: {
        callbacks: {
          title: function(tooltipItems) {
            const item = tooltipItems[0];
            if (timeRange === 'daily') {
              return `Hour: ${item.label}`;
            } else if (timeRange === 'weekly') {
              return `Week starting: ${item.label}`;
            } else {
              return item.label;
            }
          },
          label: function(context) {
            return `  Clicks: ${context.raw}`;
          }
        }
      }
    },
    scales: {
      x: {
        ticks: {
          maxRotation: timeRange === 'monthly' ? 0 : 0,
          minRotation: 0,
          autoSkip: true,
          maxTicksLimit: timeRange === 'daily' ? 24 : 
                         timeRange === 'weekly' ? 12 : 12
        },
        grid: {
          display: false
        }
      },
      y: {
        beginAtZero: true,
        ticks: {
          precision: 0 // Only show integer values
        },
        grid: {
          color: 'rgba(0, 0, 0, 0.05)'
        }
      }
    }
  };

  const pieOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'right',
        labels: {
          boxWidth: 12,
          font: {
            size: 10
          }
        }
      },
      tooltip: {
        callbacks: {
          label: function(context) {
            const label = context.label || '';
            const value = context.raw || 0;
            const total = context.chart.data.datasets[0].data.reduce((a, b) => a + b, 0);
            const percentage = Math.round((value / total) * 100);
            return `${label}: ${value} (${percentage}%)`;
          }
        }
      },
      title: {
        display: false, // We'll use our own title instead of the chart's built-in title
        font: {
          size: 14
        }
      }
    },
  };

  return (
    <>
      <Button 
        variant="outline-success" 
        size="sm" 
        onClick={() => setShowModal(true)}
        className="me-2"
      >
        <FaChartBar /> analytics
      </Button>

      <Modal 
        show={showModal} 
        onHide={() => setShowModal(false)}
        dialogClassName="analytics-modal"
        size="xl"
        centered
      >
        <Modal.Header closeButton>
          <Modal.Title>URL Analytics</Modal.Title>
        </Modal.Header>
        <Modal.Body style={{ padding: '20px 30px' }}>
          <Row className="align-items-center mb-4">
            <Col md={6}>
              <div className="d-flex align-items-center">
                <span className="me-2">
                  <FaCalendarAlt className="me-1" /> Select Date:
                </span>
                <DatePicker
                  selected={selectedDate}
                  onChange={handleDateChange}
                  className="form-control"
                  dateFormat="yyyy-MM-dd"
                  maxDate={new Date()}
                />
              </div>
            </Col>
            
            <Col md={6}>
              <div className="d-flex justify-content-end">
                <ButtonGroup className="time-range-selector">
                  <Button 
                    variant={timeRange === 'daily' ? 'primary' : 'outline-primary'} 
                    onClick={() => handleTimeRangeChange('daily')}
                  >
                    <FaCalendarDay className="me-1" /> Daily
                  </Button>
                  <Button 
                    variant={timeRange === 'weekly' ? 'primary' : 'outline-primary'} 
                    onClick={() => handleTimeRangeChange('weekly')}
                  >
                    <FaCalendarWeek className="me-1" /> Weekly
                  </Button>
                  <Button 
                    variant={timeRange === 'monthly' ? 'primary' : 'outline-primary'} 
                    onClick={() => handleTimeRangeChange('monthly')}
                  >
                    <FaCalendarAlt className="me-1" /> Monthly
                  </Button>
                </ButtonGroup>
              </div>
            </Col>
          </Row>
          
          {analyticsData && (
            <div className="d-flex justify-content-center mb-4">
              <Card className="bg-primary text-white mb-0 px-4 py-2 stats-card">
                <div className="d-flex align-items-center">
                  <FaMousePointer className="me-2" />
                  <div>
                    <h5 className="mb-0">Total Clicks: {analyticsData.total_clicks}</h5>
                    <small>Statistical Period: {timeRange === 'daily' ? 'Day' : timeRange === 'weekly' ? 'Week' : 'Month'}</small>
                  </div>
                </div>
              </Card>
            </div>
          )}

          {error && (
            <div className="alert alert-danger">{error}</div>
          )}

          {loading ? (
            <div className="text-center my-5">
              <Spinner animation="border" />
              <p className="mt-2">Loading analytics data...</p>
            </div>
          ) : (
            analyticsData && (
              <div>
                <div className="mb-4">
                  <div style={{ height: '300px', marginBottom: '40px' }}>
                    {chartData && <Line data={chartData.clicksDistribution} options={lineOptions} />}
                  </div>
                </div>
                
                <div className="row mt-4 gx-4">
                  <div className="col-md-4 mb-4">
                    <Card className="h-100 shadow-sm chart-card">
                      <Card.Body>
                        <h6 className="text-center mb-3 fw-bold">Device Distribution</h6>
                        <div className="chart-container" style={{ height: '220px' }}>
                          {chartData && <Pie data={chartData.deviceDistribution} options={pieOptions} />}
                        </div>
                      </Card.Body>
                    </Card>
                  </div>
                  
                  <div className="col-md-4 mb-4">
                    <Card className="h-100 shadow-sm chart-card">
                      <Card.Body>
                        <h6 className="text-center mb-3 fw-bold">Browser Distribution</h6>
                        <div className="chart-container" style={{ height: '220px' }}>
                          {chartData && <Pie data={chartData.browserDistribution} options={pieOptions} />}
                        </div>
                      </Card.Body>
                    </Card>
                  </div>
                  
                  <div className="col-md-4 mb-4">
                    <Card className="h-100 shadow-sm chart-card">
                      <Card.Body>
                        <h6 className="text-center mb-3 fw-bold">Country/Region Distribution</h6>
                        <div className="chart-container" style={{ height: '220px' }}>
                          {chartData && <Pie data={chartData.countryDistribution} options={pieOptions} />}
                        </div>
                      </Card.Body>
                    </Card>
                  </div>
                </div>
              </div>
            )
          )}
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setShowModal(false)}>
            Close
          </Button>
        </Modal.Footer>
      </Modal>
    </>
  );
};

export default UrlAnalytics; 