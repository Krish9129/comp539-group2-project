import React, { useState, useEffect } from 'react';
import { Modal, Button, Spinner, Form, Row, Col, Card } from 'react-bootstrap';
import { FaChartBar, FaCalendarAlt, FaMousePointer } from 'react-icons/fa';
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
import { Bar, Pie, Line } from 'react-chartjs-2';
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

  // Format date for display
  const formatDateForDisplay = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', { 
      year: 'numeric', 
      month: 'long', 
      day: 'numeric',
      weekday: 'long'
    });
  };

  // Fetch analytics data
  const fetchAnalytics = async () => {
    setLoading(true);
    setError('');
    
    try {
      const formattedDate = formatDateForApi(selectedDate);
      console.log("Requesting analytics for date:", formattedDate); // Debug log
      
      // Use shortId instead of urlId for the API call
      const data = await urlService.getUrlAnalytics(shortId, formattedDate);
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

  // When modal is opened or date changes, fetch data
  useEffect(() => {
    if (showModal) {
      fetchAnalytics();
    }
  }, [showModal, selectedDate]);

  // Handle date change
  const handleDateChange = (date) => {
    // Update the date state - this will trigger the useEffect
    setSelectedDate(date);
    console.log("Date selected:", date); // Debug log
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

    // Clicks Per Hour
    const clicksPerHourData = {
      labels: Array.from({ length: 24 }, (_, i) => `${i}:00`),
      datasets: [
        {
          label: 'Clicks',
          data: Object.values(analyticsData.clicks_per_hour),
          backgroundColor: 'rgba(54, 162, 235, 0.5)',
          borderColor: 'rgba(54, 162, 235, 1)',
          borderWidth: 1,
        },
      ],
    };
    
    // Device Distribution
    const deviceLabels = Object.keys(analyticsData.device_distribution);
    const deviceData = {
      labels: deviceLabels,
      datasets: [
        {
          data: Object.values(analyticsData.device_distribution),
          backgroundColor: getColors(deviceLabels.length),
          borderWidth: 1,
        },
      ],
    };
    
    // Country Distribution
    const countryLabels = Object.keys(analyticsData.country_distribution);
    const countryData = {
      labels: countryLabels,
      datasets: [
        {
          data: Object.values(analyticsData.country_distribution),
          backgroundColor: getColors(countryLabels.length),
          borderWidth: 1,
        },
      ],
    };
    
    // Browser Distribution
    const browserLabels = Object.keys(analyticsData.browser_distribution);
    const browserData = {
      labels: browserLabels,
      datasets: [
        {
          data: Object.values(analyticsData.browser_distribution),
          backgroundColor: getColors(browserLabels.length),
          borderWidth: 1,
        },
      ],
    };
    
    return {
      clicksPerHour: clicksPerHourData,
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
        text: 'Hourly Click Distribution',
        font: {
          size: 16
        }
      },
    },
    scales: {
      y: {
        beginAtZero: true,
        ticks: {
          precision: 0 // Only show integer values
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
        <FaChartBar /> Analytics
      </Button>

      <Modal 
        show={showModal} 
        onHide={() => setShowModal(false)}
        dialogClassName="analytics-modal"
        size="xl"
        centered
      >
        <Modal.Header closeButton>
          <Modal.Title>URL Traffic Analytics</Modal.Title>
        </Modal.Header>
        <Modal.Body style={{ padding: '20px 30px' }}>
          <div className="d-flex justify-content-between align-items-center mb-4">
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
            
            {analyticsData && (
              <div className="d-flex align-items-center">
                <Card className="bg-primary text-white mb-0 px-4 py-2 stats-card">
                  <div className="d-flex align-items-center">
                    <FaMousePointer className="me-2" />
                    <div>
                      <h5 className="mb-0">Total Clicks: {analyticsData.total_clicks}</h5>
                      <small>Date: {formatDateForApi(selectedDate)}</small>
                    </div>
                  </div>
                </Card>
              </div>
            )}
          </div>

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
                    {chartData && <Line data={chartData.clicksPerHour} options={lineOptions} />}
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
                        <h6 className="text-center mb-3 fw-bold">Country Distribution</h6>
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